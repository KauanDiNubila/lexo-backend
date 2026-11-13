package app.lexo.adapter.in.web;

import app.lexo.application.port.in.AuditEntryResponse;
import app.lexo.application.port.in.ListAuditEntriesQuery;
import app.lexo.common.security.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
class AuditController {

    private final ListAuditEntriesQuery listAuditEntries;

    AuditController(ListAuditEntriesQuery listAuditEntries) {
        this.listAuditEntries = listAuditEntries;
    }

    @GetMapping
    List<AuditEntryResponse> list(@AuthenticationPrincipal AuthUser me) {
        return listAuditEntries.latest(Identity.organization(me));
    }
}
