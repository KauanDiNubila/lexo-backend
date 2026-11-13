package app.lexo.application.service;

import app.lexo.application.port.in.AuditEntryResponse;
import app.lexo.application.port.in.ListAuditEntriesQuery;
import app.lexo.application.port.out.LoadLatestAuditEntriesPort;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class AuditEntryQueryService implements ListAuditEntriesQuery {

    private static final int LATEST = 200;

    private final LoadLatestAuditEntriesPort loadLatestAuditEntriesPort;

    AuditEntryQueryService(LoadLatestAuditEntriesPort loadLatestAuditEntriesPort) {
        this.loadLatestAuditEntriesPort = loadLatestAuditEntriesPort;
    }

    @Override
    public List<AuditEntryResponse> latest(OrganizationId organizationId) {
        return loadLatestAuditEntriesPort.latest(organizationId, LATEST).stream()
                .map(AuditEntryResponse::of)
                .toList();
    }
}
