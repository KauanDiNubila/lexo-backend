package app.lexo.controller;

import app.lexo.common.referencia.ExisteResponse;
import app.lexo.repository.ClientRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalClientController {

    private final ClientRepository repo;

    public InternalClientController(ClientRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/internal/clientes/{id}/existe")
    public ExisteResponse existe(@PathVariable String id, @RequestParam String orgId) {
        return new ExisteResponse(repo.existsByIdAndOrganizationId(id, orgId));
    }
}
