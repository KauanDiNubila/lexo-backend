package app.lexo.controller;

import app.lexo.common.reference.ExistsResponse;
import app.lexo.repository.CaseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalCaseController {

    private final CaseRepository repo;

    public InternalCaseController(CaseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/internal/processos/{id}/existe")
    public ExistsResponse existe(@PathVariable String id, @RequestParam String orgId) {
        return new ExistsResponse(repo.existsByIdAndOrganizationId(id, orgId));
    }
}
