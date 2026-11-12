package app.lexo.domain;

import java.time.Instant;
import java.util.Optional;

public class Deadline {

    private static final double MILLIS_PER_DAY = 1000.0 * 60 * 60 * 24;

    private final DeadlineId id;
    private final OrganizationId organizationId;
    private DeadlineData data;
    private DeadlineStatus status;
    private Instant notifiedAt;

    private Deadline(DeadlineId id, OrganizationId organizationId, DeadlineData data, DeadlineStatus status,
                     Instant notifiedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.data = data;
        this.status = status;
        this.notifiedAt = notifiedAt;
    }

    public static Deadline schedule(OrganizationId organizationId, DeadlineData data) {
        return new Deadline(null, organizationId, data, DeadlineStatus.PENDING, null);
    }

    public static Deadline restore(DeadlineId id, OrganizationId organizationId, DeadlineData data,
                                   DeadlineStatus status, Instant notifiedAt) {
        return new Deadline(id, organizationId, data, status, notifiedAt);
    }

    public void update(DeadlineData newData) {
        this.data = newData;
    }

    public void complete() {
        this.status = DeadlineStatus.COMPLETED;
    }

    public void reopen() {
        this.status = DeadlineStatus.PENDING;
    }

    public void markNotified(Instant now) {
        this.notifiedAt = now;
    }

    public Optional<RiskLevel> riskAt(Instant now) {
        if (status != DeadlineStatus.PENDING) {
            return Optional.empty();
        }
        double daysRemaining = (data.date().toEpochMilli() - now.toEpochMilli()) / MILLIS_PER_DAY;
        return Optional.of(RiskLevel.forDaysRemaining(daysRemaining / data.type().riskFactor()));
    }

    public DeadlineId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public DeadlineData data() {
        return data;
    }

    public DeadlineStatus status() {
        return status;
    }

    public Instant notifiedAt() {
        return notifiedAt;
    }
}
