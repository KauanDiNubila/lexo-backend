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
public class CaseService {

    private final CaseRepository repo;
    private final ClientRepository clientRepo;
    private final UserRepository userRepo;
    private final ActivityService activity;

    public CaseService(CaseRepository repo, ClientRepository clientRepo,
                       UserRepository userRepo, ActivityService activity) {
        this.repo = repo;
        this.clientRepo = clientRepo;
        this.userRepo = userRepo;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> list(AuthUser me) {
        return repo.findByOrganizationIdOrderByCreatedAtDesc(me.organizationId())
                .stream().map(CaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse get(AuthUser me, String id) {
        return CaseResponse.from(load(me, id));
    }

    @Transactional
    public CaseResponse create(AuthUser me, CaseRequest req) {
        validateRefs(me, req);
        Case c = new Case();
        c.setOrganizationId(me.organizationId());
        apply(c, req);
        c = repo.save(c);

        activity.log(me.organizationId(), c.getId(), me.id(), nameOf(me), "Processo criado");
        return CaseResponse.from(c);
    }

    @Transactional
    public CaseResponse update(AuthUser me, String id, CaseRequest req) {
        validateRefs(me, req);
        Case c = load(me, id);
        apply(c, req);
        c = repo.save(c);

        activity.log(me.organizationId(), c.getId(), me.id(), nameOf(me),
                "Processo atualizado — status: " + req.status());
        return CaseResponse.from(c);
    }

    @Transactional
    public void delete(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private Case load(AuthUser me, String id) {
        return repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Processo não encontrado"));
    }

    private void validateRefs(AuthUser me, CaseRequest req) {
        if (!clientRepo.existsByIdAndOrganizationId(req.clientId(), me.organizationId())) {
            throw ApiException.notFound("Cliente não encontrado");
        }
        if (req.responsavelId() != null && !req.responsavelId().isBlank()
                && userRepo.findByIdAndOrganizationId(req.responsavelId(), me.organizationId()).isEmpty()) {
            throw ApiException.notFound("Responsável não encontrado");
        }
    }

    private void apply(Case c, CaseRequest req) {
        c.setClientId(req.clientId());
        c.setNumber(req.number());
        c.setArea(blankToNull(req.area()));
        c.setStatus(req.status());
        c.setDescription(blankToNull(req.description()));
        c.setResponsavelId(blankToNull(req.responsavelId()));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String nameOf(AuthUser me) {
        return me.name() != null ? me.name() : "Usuário";
    }
}
