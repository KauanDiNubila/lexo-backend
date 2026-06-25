package app.lexo.service;

import app.lexo.domain.Case;
import app.lexo.dto.CaseDtos.CaseRequest;
import app.lexo.dto.CaseDtos.CaseResponse;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.ClientRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.AuthUser;
import app.lexo.controller.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessoService {

    private final CaseRepository repo;
    private final ClientRepository clientRepo;
    private final UserRepository userRepo;
    private final AtividadeService activity;

    public ProcessoService(CaseRepository repo, ClientRepository clientRepo,
                       UserRepository userRepo, AtividadeService activity) {
        this.repo = repo;
        this.clientRepo = clientRepo;
        this.userRepo = userRepo;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listar(AuthUser me) {
        return repo.findByOrganizationIdOrderByCreatedAtDesc(me.organizationId())
                .stream().map(CaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse buscar(AuthUser me, String id) {
        return CaseResponse.from(carregar(me, id));
    }

    @Transactional
    public CaseResponse criar(AuthUser me, CaseRequest req) {
        validarReferencias(me, req);
        Case c = new Case();
        c.setOrganizationId(me.organizationId());
        preencher(c, req);
        c = repo.save(c);

        activity.registrar(me.organizationId(), c.getId(), me.id(), nomeDe(me), "Processo criado");
        return CaseResponse.from(c);
    }

    @Transactional
    public CaseResponse atualizar(AuthUser me, String id, CaseRequest req) {
        validarReferencias(me, req);
        Case c = carregar(me, id);
        preencher(c, req);
        c = repo.save(c);

        activity.registrar(me.organizationId(), c.getId(), me.id(), nomeDe(me),
                "Processo atualizado — status: " + req.status());
        return CaseResponse.from(c);
    }

    @Transactional
    public void excluir(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private Case carregar(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Processo não encontrado"));
    }

    private void validarReferencias(AuthUser me, CaseRequest req) {
        if (!clientRepo.existsByIdAndOrganizationId(req.clientId(), me.organizationId())) {
            throw ApiException.notFound("Cliente não encontrado");
        }
        if (req.responsavelId() != null && !req.responsavelId().isBlank()
                && userRepo.findByIdAndOrganizationId(req.responsavelId(), me.organizationId()).isEmpty()) {
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
