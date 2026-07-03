package app.lexo.controller;

import app.lexo.repository.ClientRepository;
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
public class InternalClientController {

    private final ClientRepository repo;
    private final String chaveInterna;

    public InternalClientController(ClientRepository repo,
                                    @Value("${lexo.internal.api-key}") String chaveInterna) {
        this.repo = repo;
        this.chaveInterna = chaveInterna;
    }

    @GetMapping("/internal/clientes/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId,
                                       @RequestHeader(value = "X-Internal-Key", required = false) String chave) {
        if (!chaveInterna.equals(chave)) {
            throw ApiException.forbidden("Acesso interno negado");
        }
        return Map.of("existe", repo.existsByIdAndOrganizationId(id, orgId));
    }
}
