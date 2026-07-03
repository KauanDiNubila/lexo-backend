package app.lexo.service;

import app.lexo.controller.ApiException;
import app.lexo.domain.Andamento;
import app.lexo.dto.AndamentoDtos.AndamentoRequest;
import app.lexo.dto.AndamentoDtos.AndamentoResponse;
import app.lexo.repository.AndamentoRepository;
import app.lexo.repository.CaseRepository;
import app.lexo.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AndamentoService {

    private final AndamentoRepository repo;
    private final CaseRepository caseRepo;

    public AndamentoService(AndamentoRepository repo, CaseRepository caseRepo) {
        this.repo = repo;
        this.caseRepo = caseRepo;
    }

    @Transactional(readOnly = true)
    public List<AndamentoResponse> listar(AuthUser me, String caseId) {
        exigirProcessoProprio(me, caseId);
        return repo.findByCaseIdAndOrganizationIdOrderByDateDesc(caseId, me.organizationId())
                .stream().map(AndamentoResponse::from).toList();
    }

    @Transactional
    public AndamentoResponse criar(AuthUser me, String caseId, AndamentoRequest req) {
        exigirProcessoProprio(me, caseId);
        Andamento a = new Andamento();
        a.setOrganizationId(me.organizationId());
        a.setCaseId(caseId);
        a.setTitle(req.title());
        a.setDescription(req.description());
        a.setDate(req.date());
        return AndamentoResponse.from(repo.save(a));
    }

    @Transactional
    public void excluir(AuthUser me, String caseId, String id) {
        exigirProcessoProprio(me, caseId);
        Andamento a = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Andamento não encontrado"));
        repo.delete(a);
    }

    private void exigirProcessoProprio(AuthUser me, String caseId) {
        if (!caseRepo.existsByIdAndOrganizationId(caseId, me.organizationId())) {
            throw ApiException.notFound("Processo não encontrado");
        }
    }
}
