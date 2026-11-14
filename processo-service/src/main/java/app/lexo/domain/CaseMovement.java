package app.lexo.domain;

import java.time.Instant;

public class CaseMovement {

    private final MovementId id;
    private final OrganizationId organizationId;
    private final CaseId caseId;
    private final MovementData data;
    private final Instant createdAt;

    private CaseMovement(MovementId id, OrganizationId organizationId, CaseId caseId, MovementData data,
                         Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.caseId = caseId;
        this.data = data;
        this.createdAt = createdAt;
    }

    public static CaseMovement record(OrganizationId organizationId, CaseId caseId, MovementData data) {
        return new CaseMovement(null, organizationId, caseId, data, null);
    }

    public static CaseMovement restore(MovementId id, OrganizationId organizationId, CaseId caseId,
                                       MovementData data, Instant createdAt) {
        return new CaseMovement(id, organizationId, caseId, data, createdAt);
    }

    public MovementId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public CaseId caseId() {
        return caseId;
    }

    public MovementData data() {
        return data;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
