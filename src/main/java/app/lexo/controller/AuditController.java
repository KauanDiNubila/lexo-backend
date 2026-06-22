package app.lexo.controller;

import app.lexo.dto.TotpDtos.AuditLogResponse;
import app.lexo.repository.AuditLogRepository;
import app.lexo.security.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Log de auditoria — restrito a ADMIN. */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AuditLogResponse> list(@AuthenticationPrincipal AuthUser me) {
        return repo.findTop200ByOrganizationIdOrderByCreatedAtDesc(me.organizationId())
                .stream().map(AuditLogResponse::from).toList();
    }
}
