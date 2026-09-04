package app.lexo.service;

import app.lexo.client.ReferenciaGateway;
import app.lexo.common.evento.EventoDominio;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Case;
import app.lexo.domain.enums.CaseStatus;
import app.lexo.dto.CaseDtos.CaseOpcao;
import app.lexo.dto.CaseDtos.CaseRequest;
import app.lexo.dto.CaseDtos.CaseResponse;
import app.lexo.dto.CaseDtos.ResumoProcessos;
import app.lexo.common.kafka.PublicadorEventos;
import app.lexo.repository.AndamentoRepository;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.CaseSpecs;
import app.lexo.repository.DeadlineRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoService {

    private static final String CACHE_OPCOES = "processos-opcoes";

    private final CaseRepository repo;
    private final DeadlineRepository prazos;
    private final AndamentoRepository andamentos;
    private final ReferenciaGateway referencias;
    private final PublicadorEventos eventos;

    public ProcessoService(CaseRepository repo, DeadlineRepository prazos,
                       AndamentoRepository andamentos, ReferenciaGateway referencias,
                       PublicadorEventos eventos) {
        this.repo = repo;
        this.prazos = prazos;
        this.andamentos = andamentos;
        this.referencias = referencias;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Pagina<CaseResponse> listar(AuthUser me, String busca, CaseStatus status, Pageable pageable) {
        var filtro = CaseSpecs.daOrganizacao(me.organizationId())
                .and(CaseSpecs.comStatus(status))
                .and(CaseSpecs.busca(busca));
        return Pagina.de(repo.findAll(filtro, pageable), CaseResponse::from);
    }

    @Cacheable(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional(readOnly = true)
    public List<CaseOpcao> opcoes(AuthUser me) {
        return repo.findByOrganizationIdOrderByCreatedAtDesc(me.organizationId())
                .stream().map(CaseOpcao::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumoProcessos resumo(AuthUser me) {
        String org = me.organizationId();
        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            porStatus.put(status.name(), repo.countByOrganizationIdAndStatus(org, status));
        }
        return new ResumoProcessos(
                repo.countByOrganizationId(org),
                porStatus.getOrDefault(CaseStatus.ATIVO.name(), 0L),
                porStatus);
    }

    @Transactional(readOnly = true)
    public CaseResponse buscar(AuthUser me, String id) {
        return CaseResponse.from(carregar(me, id));
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public CaseResponse criar(AuthUser me, CaseRequest req) {
        validarReferencias(me, req);
        Case c = new Case();
        c.setOrganizationId(me.organizationId());
        preencher(c, req);
        c = repo.save(c);

        eventos.publicar(EventoDominio.de("PROCESSO_CRIADO", me.organizationId(), "PROCESSO",
                c.getId(), "Processo criado: " + c.getNumber(), me.id(), nomeDe(me)));
        return CaseResponse.from(c);
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public CaseResponse atualizar(AuthUser me, String id, CaseRequest req) {
        validarReferencias(me, req);
        Case c = carregar(me, id);
        preencher(c, req);
        c = repo.save(c);

        eventos.publicar(EventoDominio.de("PROCESSO_ATUALIZADO", me.organizationId(), "PROCESSO",
                c.getId(), "Processo atualizado: " + c.getNumber(), me.id(), nomeDe(me)));
        return CaseResponse.from(c);
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public void excluir(AuthUser me, String id) {
        Case c = carregar(me, id);
        apagarComFilhos(c);

        eventos.publicar(EventoDominio.de("PROCESSO_EXCLUIDO", me.organizationId(), "PROCESSO",
                c.getId(), "Processo excluído: " + c.getNumber(), me.id(), nomeDe(me)));
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#organizationId")
    @Transactional
    public int excluirPorCliente(String organizationId, String clientId) {
        List<Case> casos = repo.findByClientIdAndOrganizationId(clientId, organizationId);
        casos.forEach(this::apagarComFilhos);
        return casos.size();
    }

    private void apagarComFilhos(Case c) {
        prazos.deleteByCaseIdAndOrganizationId(c.getId(), c.getOrganizationId());
        andamentos.deleteByCaseIdAndOrganizationId(c.getId(), c.getOrganizationId());
        repo.delete(c);
    }

    private Case carregar(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Processo não encontrado"));
    }

    private void validarReferencias(AuthUser me, CaseRequest req) {

        if (!referencias.clienteExiste(req.clientId(), me.organizationId())) {
            throw ApiException.notFound("Cliente não encontrado");
        }

        if (req.responsavelId() != null && !req.responsavelId().isBlank()
                && !referencias.usuarioExiste(req.responsavelId(), me.organizationId())) {
            throw ApiException.notFound("Responsável não encontrado");
        }
    }

    private void preencher(Case c, CaseRequest req) {
        c.setClientId(req.clientId());
        c.setNumber(req.number());
        c.setArea(vazioParaNulo(req.area()));
        c.setStatus(req.status());
        c.setDescription(vazioParaNulo(req.description()));
        c.setResponsavelId(vazioParaNulo(req.responsavelId()));
    }

    private String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String nomeDe(AuthUser me) {
        return me.name() != null ? me.name() : "Usuário";
    }
}
