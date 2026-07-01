package app.lexo.controller;

import app.lexo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    private final String chaveInterna;

    public InternalUserController(UserRepository userRepo,
                                  @Value("${lexo.internal.api-key}") String chaveInterna) {
        this.userRepo = userRepo;
        this.chaveInterna = chaveInterna;
    }

    @GetMapping("/internal/usuarios/{id}/existe")
    public Map<String, Boolean> existe(@PathVariable String id, @RequestParam String orgId,
                                       @RequestHeader(value = "X-Internal-Key", required = false) String chave) {
        if (!chaveInterna.equals(chave)) {
            throw ApiException.forbidden("Acesso interno negado");
        }
        return Map.of("existe", userRepo.existsByIdAndOrganizationId(id, orgId));
    }
}
