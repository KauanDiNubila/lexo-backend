package app.lexo.service;

import app.lexo.domain.Deadline;
import app.lexo.dto.DeadlineDtos.DeadlineRequest;
import app.lexo.dto.DeadlineDtos.DeadlineResponse;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import app.lexo.security.AuthUser;
import app.lexo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeadlineService {

    private final DeadlineRepository repo;
    private final CaseRepository caseRepo;
    private final ActivityService activity;

    public DeadlineService(DeadlineRepository repo, CaseRepository caseRepo, ActivityService activity) {
        this.repo = repo;
        this.caseRepo = caseRepo;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<DeadlineResponse> list(AuthUser me) {
        return repo.findByOrganizationIdOrderByDateAsc(me.organizationId())
                .stream().map(DeadlineResponse::from).toList();
    }

    @Transactional
    public DeadlineResponse create(AuthUser me, DeadlineRequest req) {
        requireOwnCase(me, req.caseId());
        Deadline d = new Deadline();
        d.setOrganizationId(me.organizationId());
        d.setCaseId(req.caseId());
        apply(d, req);
        d = repo.save(d);

        activity.log(me.organizationId(), d.getCaseId(), me.id(), nameOf(me),
                "Prazo \"" + req.title() + "\" criado");
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse update(AuthUser me, String id, DeadlineRequest req) {
        requireOwnCase(me, req.caseId());
        Deadline d = load(me, id);
        d.setCaseId(req.caseId());
        apply(d, req);
        d = repo.save(d);

        activity.log(me.organizationId(), d.getCaseId(), me.id(), nameOf(me),
                "Prazo \"" + req.title() + "\" atualizado");
        return DeadlineResponse.from(d);
    }

    @Transactional
    public DeadlineResponse toggleStatus(AuthUser me, String id, boolean completed) {
        Deadline d = load(me, id);
        d.setStatus(completed ? DeadlineStatus.CONCLUIDO : DeadlineStatus.PENDENTE);
        d = repo.save(d);

        activity.log(me.organizationId(), d.getCaseId(), me.id(), nameOf(me),
                "Prazo \"" + d.getTitle() + "\" marcado como " + (completed ? "Concluído" : "Pendente"));
        return DeadlineResponse.from(d);
    }

    @Transactional
    public void delete(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private Deadline load(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Prazo não encontrado"));
    }

    private void requireOwnCase(AuthUser me, String caseId) {
        if (!caseRepo.existsByIdAndOrganizationId(caseId, me.organizationId())) {
            throw ApiException.notFound("Processo não encontrado");
        }
    }

    private void apply(Deadline d, DeadlineRequest req) {
        d.setTitle(req.title());
        d.setType(req.type());
        d.setDate(req.date());
        d.setDescription(blankToNull(req.description()));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String nameOf(AuthUser me) {
        return me.name() != null ? me.name() : "Usuário";
    }
}
