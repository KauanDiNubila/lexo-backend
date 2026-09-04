package app.lexo.service;

import app.lexo.domain.Deadline;
import app.lexo.dto.DeadlineDtos.DeadlineRequest;
import app.lexo.dto.DeadlineDtos.DeadlineResponse;
import app.lexo.dto.DeadlineDtos.ResumoAgenda;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.Pagina;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AgendaService {

    private final DeadlineRepository repo;
    private final CaseRepository caseRepo;

    public AgendaService(DeadlineRepository repo, CaseRepository caseRepo) {
        this.repo = repo;
        this.caseRepo = caseRepo;
    }

    @Transactional(readOnly = true)
    public Pagina<DeadlineResponse> listar(AuthUser me, String caseId, Pageable pageable) {
        var pagina = (caseId == null || caseId.isBlank())
                ? repo.findByOrganizationId(me.organizationId(), pageable)
                : repo.findByOrganizationIdAndCaseId(me.organizationId(), caseId, pageable);
        return Pagina.de(pagina, DeadlineResponse::from);
    }

    @Transactional(readOnly = true)
    public ResumoAgenda resumo(AuthUser me) {
        String org = me.organizationId();
        Instant agora = Instant.now();
        Instant em7dias = agora.plus(7, ChronoUnit.DAYS);

        return new ResumoAgenda(
                repo.countByOrganizationIdAndStatusAndDateBetween(
                        org, DeadlineStatus.PENDENTE, agora, em7dias),
                repo.countByOrganizationIdAndStatusAndDateBefore(
                        org, DeadlineStatus.PENDENTE, agora),
                repo.findTop5ByOrganizationIdAndStatusOrderByDateAsc(org, DeadlineStatus.PENDENTE)
                        .stream().map(DeadlineResponse::from).toList());
    }

    @Transactional
    public DeadlineResponse criar(AuthUser me, DeadlineRequest req) {
        exigirProcessoProprio(me, req.caseId());
        Deadline d = new Deadline();
        d.setOrganizationId(me.organizationId());
        d.setCaseId(req.caseId());
        preencher(d, req);
        d = repo.save(d);
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse atualizar(AuthUser me, String id, DeadlineRequest req) {
        exigirProcessoProprio(me, req.caseId());
        Deadline d = carregar(me, id);
        d.setCaseId(req.caseId());
        preencher(d, req);
        d = repo.save(d);
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse alternarStatus(AuthUser me, String id, boolean completed) {
        Deadline d = carregar(me, id);
        d.setStatus(completed ? DeadlineStatus.CONCLUIDO : DeadlineStatus.PENDENTE);
        d = repo.save(d);
        return DeadlineResponse.from(d);
    }

    @Transactional
    public void excluir(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private Deadline carregar(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Prazo não encontrado"));
    }

    private void exigirProcessoProprio(AuthUser me, String caseId) {
        if (!caseRepo.existsByIdAndOrganizationId(caseId, me.organizationId())) {
            throw ApiException.notFound("Processo não encontrado");
        }
    }

    private void preencher(Deadline d, DeadlineRequest req) {
        d.setTitle(req.title());
        d.setType(req.type());
        d.setDate(req.date());
        d.setDescription(vazioParaNulo(req.description()));
    }

    private String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
