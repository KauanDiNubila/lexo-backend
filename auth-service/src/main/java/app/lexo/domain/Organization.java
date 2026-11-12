package app.lexo.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Organization {

    private static final String TRIAL_PLAN = "trial";
    private static final long TRIAL_DAYS = 30;

    private final OrganizationId id;
    private String name;
    private final String plan;
    private final Instant trialEndsAt;

    private Organization(OrganizationId id, String name, String plan, Instant trialEndsAt) {
        this.id = id;
        this.name = name;
        this.plan = plan;
        this.trialEndsAt = trialEndsAt;
    }

    public static Organization register(String name, Instant now) {
        return new Organization(null, name, TRIAL_PLAN, now.plus(TRIAL_DAYS, ChronoUnit.DAYS));
    }

    public static Organization restore(OrganizationId id, String name, String plan, Instant trialEndsAt) {
        return new Organization(id, name, plan, trialEndsAt);
    }

    public void rename(String newName) {
        this.name = newName.trim();
    }

    public OrganizationId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String plan() {
        return plan;
    }

    public Instant trialEndsAt() {
        return trialEndsAt;
    }
}
