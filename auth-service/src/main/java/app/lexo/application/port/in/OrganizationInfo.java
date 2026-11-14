package app.lexo.application.port.in;

import app.lexo.domain.Organization;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OrganizationInfo(String id, String name, String plan, Instant trialEndsAt,
                               @JsonProperty("membros") int members) {

    public static OrganizationInfo of(Organization organization, int members) {
        return new OrganizationInfo(organization.id().value(), organization.name(), organization.plan(),
                organization.trialEndsAt(), members);
    }
}
