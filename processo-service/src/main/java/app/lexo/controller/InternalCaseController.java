package app.lexo.controller;

import app.lexo.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class InternalCaseController {

    private final CaseRepository repo;
    private final String chaveInterna;

    public InternalCaseController(CaseRepository repo,
                                  @Value("${lexo.internal.api-key}") String chaveInterna) {
        this.repo = repo;
        this.chaveInterna = chaveInterna;
    }

    @GetMapping("/internal/processos/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId,
                                       @RequestHeader(value = "X-Internal-Key", required = false) String chave) {
        if (!chaveInterna.equals(chave)) {
            throw ApiException.forbidden("Acesso interno negado");
        }
        return Map.of("existe", repo.existsByIdAndOrganizationId(id, orgId));
    }
}
