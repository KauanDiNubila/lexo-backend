package app.lexo.domain;

import app.lexo.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserInvite")
class UserInviteTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private UserInvite issued() {
        return UserInvite.issue(new OrganizationId("org-1"), "Carla", new EmailAddress("carla@escritorio.com"),
                Role.ADVOGADO, NOW);
    }

    @Test
    @DisplayName("an invite is valid for seven days and carries a random token")
    void validForSevenDays() {
        UserInvite invite = issued();

        assertThat(invite.expiresAt()).isEqualTo(Instant.parse("2026-01-08T12:00:00Z"));
        assertThat(invite.token()).matches("[0-9a-f]{32}").isNotEqualTo(issued().token());
        assertThat(invite.statusAt(NOW)).isEqualTo(InviteStatus.PENDING);
    }

    @Test
    @DisplayName("it is still pending at the expiry instant and expired right after")
    void expiryBoundary() {
        UserInvite invite = issued();

        assertThat(invite.statusAt(invite.expiresAt())).isEqualTo(InviteStatus.PENDING);
        assertThat(invite.statusAt(invite.expiresAt().plusMillis(1))).isEqualTo(InviteStatus.EXPIRED);
    }

    @Test
    @DisplayName("an accepted invite stays accepted even after it would have expired")
    void acceptedWinsOverExpired() {
        UserInvite invite = issued();

        invite.accept(NOW.plusSeconds(10));

        assertThat(invite.acceptedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(invite.statusAt(NOW.plusSeconds(30L * 86400))).isEqualTo(InviteStatus.ACCEPTED);
    }
}
