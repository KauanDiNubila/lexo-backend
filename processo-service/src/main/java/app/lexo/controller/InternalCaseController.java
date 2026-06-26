package app.lexo.controller;

import app.lexo.repository.CaseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints internos (servico-a-servico). Usados por outros servicos para validar a
 * existencia de um processo (ex.: ao criar um honorario vinculado a um processo).
 */
@RestController
public class InternalCaseController {

    private final CaseRepository repo;

    public InternalCaseController(CaseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/internal/processos/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId) {
        return Map.of("existe", repo.existsByIdAndOrganizationId(id, orgId));
    }
}
