package app.lexo.controller;

import app.lexo.common.reference.ExistsResponse;
import app.lexo.common.user.InternalUser;
import app.lexo.common.web.ApiException;
import app.lexo.domain.User;
import app.lexo.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalUserController {

    private final UserRepository userRepo;

    public InternalUserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/internal/usuarios/{id}/existe")
    public ExistsResponse existe(@PathVariable String id, @RequestParam String orgId) {
        return new ExistsResponse(userRepo.existsByIdAndOrganizationId(id, orgId));
    }

    @GetMapping("/internal/usuarios/{id}")
    public InternalUser buscar(@PathVariable String id, @RequestParam String orgId) {
        User u = userRepo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        return new InternalUser(u.getId(), u.getName(), u.getEmail());
    }
}
