package app.lexo.domain;

import java.time.Instant;

public class Case {

    private final CaseId id;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private CaseData data;

    private Case(CaseId id, OrganizationId organizationId, CaseData data, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.data = data;
        this.createdAt = createdAt;
    }

    public static Case open(OrganizationId organizationId, CaseData data) {
        return new Case(null, organizationId, data, null);
    }

    public static Case restore(CaseId id, OrganizationId organizationId, CaseData data, Instant createdAt) {
        return new Case(id, organizationId, data, createdAt);
    }

    public void update(CaseData newData) {
        this.data = newData;
    }

    public CaseId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public CaseData data() {
        return data;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
