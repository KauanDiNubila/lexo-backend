package app.lexo.service;

import app.lexo.domain.Deadline;
import app.lexo.dto.DeadlineDtos.DeadlineRequest;
import app.lexo.dto.DeadlineDtos.DeadlineResponse;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import app.lexo.security.AuthUser;
import app.lexo.controller.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgendaService {

    private final DeadlineRepository repo;
    private final CaseRepository caseRepo;
    private final AtividadeService activity;

    public AgendaService(DeadlineRepository repo, CaseRepository caseRepo, AtividadeService activity) {
        this.repo = repo;
        this.caseRepo = caseRepo;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<DeadlineResponse> listar(AuthUser me) {
        return repo.findByOrganizationIdOrderByDateAsc(me.organizationId())
                .stream().map(DeadlineResponse::from).toList();
    }

    @Transactional
    public DeadlineResponse criar(AuthUser me, DeadlineRequest req) {
        exigirProcessoProprio(me, req.caseId());
        Deadline d = new Deadline();
        d.setOrganizationId(me.organizationId());
        d.setCaseId(req.caseId());
        preencher(d, req);
        d = repo.save(d);

        activity.registrar(me.organizationId(), d.getCaseId(), me.id(), nomeDe(me),
                "Prazo \"" + req.title() + "\" criado");
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse atualizar(AuthUser me, String id, DeadlineRequest req) {
        exigirProcessoProprio(me, req.caseId());
        Deadline d = carregar(me, id);
        d.setCaseId(req.caseId());
        preencher(d, req);
        d = repo.save(d);

        activity.registrar(me.organizationId(), d.getCaseId(), me.id(), nomeDe(me),
                "Prazo \"" + req.title() + "\" atualizado");
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse alternarStatus(AuthUser me, String id, boolean completed) {
        Deadline d = carregar(me, id);
        d.setStatus(completed ? DeadlineStatus.CONCLUIDO : DeadlineStatus.PENDENTE);
        d = repo.save(d);

        activity.registrar(me.organizationId(), d.getCaseId(), me.id(), nomeDe(me),
                "Prazo \"" + d.getTitle() + "\" marcado como " + (completed ? "Concluído" : "Pendente"));
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

    private String nomeDe(AuthUser me) {
        return me.name() != null ? me.name() : "Usuário";
    }
}
