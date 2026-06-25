package app.lexo.controller;

import app.lexo.repository.ClientRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints internos (servico-a-servico), fora do fluxo do gateway. Usados por outros
 * servicos para validar a existencia de um cliente (ex.: ao criar um processo ou honorario).
 */
@RestController
public class InternalClientController {

    private final ClientRepository repo;

    public InternalClientController(ClientRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/internal/clientes/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId) {
        return Map.of("existe", repo.existsByIdAndOrganizationId(id, orgId));
    }
}
