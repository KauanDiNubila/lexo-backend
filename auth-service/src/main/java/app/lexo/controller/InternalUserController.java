package app.lexo.controller;

import app.lexo.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
