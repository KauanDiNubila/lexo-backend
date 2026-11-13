package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Client")
class ClientTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    private ClientData data(String name) {
        return new ClientData(name, null, "maria@escritorio.com", "11999990000", "obs");
    }

    @Test
    @DisplayName("blank e-mail, phone and notes become null")
    void blankFieldsBecomeNull() {
        ClientData data = new ClientData("Maria", null, " ", "", "  ");

        assertThat(data.email()).isNull();
        assertThat(data.phone()).isNull();
        assertThat(data.notes()).isNull();
    }

    @Test
    @DisplayName("register starts without id and without portal link")
    void register() {
        Client client = Client.register(ORGANIZATION, data("Maria Souza"));

        assertThat(client.id()).isNull();
        assertThat(client.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(client.name()).isEqualTo("Maria Souza");
        assertThat(client.portalLink()).isNull();
        assertThat(client.portalActiveAt(NOW)).isFalse();
    }

    @Test
    @DisplayName("update replaces the data and keeps the portal link")
    void update() {
        Client client = Client.register(ORGANIZATION, data("Maria Souza"));
        client.generatePortalLink("abc", NOW, 90);

        client.update(data("Maria Souza Lima"));

        assertThat(client.name()).isEqualTo("Maria Souza Lima");
        assertThat(client.portalLink().tokenHash()).isEqualTo("abc");
    }

    @Test
    @DisplayName("the portal link is valid for the agreed days and then expires")
    void linkValidity() {
        Client client = Client.register(ORGANIZATION, data("Maria Souza"));

        PortalLink link = client.generatePortalLink("abc", NOW, 90);

        assertThat(link.expiresAt()).isEqualTo(Instant.parse("2027-02-09T12:00:00Z"));
        assertThat(client.portalActiveAt(NOW.plusSeconds(60))).isTrue();
        assertThat(client.portalActiveAt(Instant.parse("2027-02-09T11:59:59Z"))).isTrue();
        assertThat(client.portalActiveAt(Instant.parse("2027-02-09T12:00:00Z"))).isFalse();
        assertThat(client.portalActiveAt(Instant.parse("2027-03-01T00:00:00Z"))).isFalse();
    }

    @Test
    @DisplayName("generating a new link replaces the previous one")
    void newLinkReplacesPrevious() {
        Client client = Client.register(ORGANIZATION, data("Maria Souza"));
        client.generatePortalLink("primeiro", NOW, 90);

        client.generatePortalLink("segundo", NOW, 90);

        assertThat(client.portalLink().tokenHash()).isEqualTo("segundo");
    }

    @Test
    @DisplayName("revoke removes the link")
    void revoke() {
        Client client = Client.register(ORGANIZATION, data("Maria Souza"));
        client.generatePortalLink("abc", NOW, 90);

        client.revokePortalLink();

        assertThat(client.portalLink()).isNull();
        assertThat(client.portalActiveAt(NOW)).isFalse();
    }

    @Test
    @DisplayName("a link without expiry is never active")
    void linkWithoutExpiry() {
        Client client = Client.restore(new ClientId("c1"), ORGANIZATION, data("Maria Souza"),
                new PortalLink("abc", null), NOW);

        assertThat(client.portalActiveAt(NOW)).isFalse();
    }
}
