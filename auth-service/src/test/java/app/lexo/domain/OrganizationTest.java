package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Organization")
class OrganizationTest {

    @Test
    @DisplayName("a new organization starts a 30 day trial")
    void startsTheTrial() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");

        Organization organization = Organization.register("Escritorio Silva", now);

        assertThat(organization.id()).isNull();
        assertThat(organization.plan()).isEqualTo("trial");
        assertThat(organization.trialEndsAt()).isEqualTo(Instant.parse("2026-01-31T12:00:00Z"));
        assertThat(organization.name()).isEqualTo("Escritorio Silva");
    }

    @Test
    @DisplayName("rename trims the name")
    void renameTrims() {
        Organization organization = Organization.register("Escritorio Silva", Instant.now());

        organization.rename("  Novo Nome ");

        assertThat(organization.name()).isEqualTo("Novo Nome");
    }
}
