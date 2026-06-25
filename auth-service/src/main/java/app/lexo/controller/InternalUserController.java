package app.lexo.controller;

import app.lexo.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints internos (servico-a-servico), fora do fluxo do gateway. Usados por outros
 * servicos para consultar dados de usuario — ex.: validar o responsavel de um processo.
 *
 * Em producao, /internal/** deve ser protegido por autenticacao servico-a-servico
 * (mTLS, API key) e nao exposto pelo gateway. Aqui fica acessivel apenas na rede interna.
 */
@RestController
public class InternalUserController {

    private final UserRepository userRepo;

    public InternalUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/internal/usuarios/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId) {
        return Map.of("existe", userRepo.existsByIdAndOrganizationId(id, orgId));
    }
}
