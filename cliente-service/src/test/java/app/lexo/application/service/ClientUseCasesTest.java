package app.lexo.application.service;

import app.lexo.application.PortalSettings;
import app.lexo.application.port.in.UpdateClientCommand;
import app.lexo.application.port.in.Actor;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.in.CreateClientCommand;
import app.lexo.application.port.in.DeleteClientCommand;
import app.lexo.application.port.in.GeneratePortalLinkCommand;
import app.lexo.application.port.in.PortalLinkResponse;
import app.lexo.application.port.in.RevokePortalLinkCommand;
import app.lexo.application.port.out.ClientEvent;
import app.lexo.common.error.InvalidDataException;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.TaxId;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.PortalToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static app.lexo.application.service.InMemoryClients.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Client use cases (in-memory ports)")
class ClientUseCasesTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");
    private static final OrganizationId OTHER_ORGANIZATION = new OrganizationId("escritorio-2");
    private static final Actor ACTOR = new Actor("usuario-1", "Ana Advogada");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryClients clients = new InMemoryClients();
    private final InMemoryEvents events = new InMemoryEvents();

    private final CreateClientService create = new CreateClientService(clients, events, clock);
    private final UpdateClientService update = new UpdateClientService(clients, clients, events, clock);
    private final DeleteClientService delete = new DeleteClientService(clients, clients, events);
    private final GeneratePortalLinkService generateLink =
            new GeneratePortalLinkService(clients, clients, clock, new PortalSettings(90));
    private final RevokePortalLinkService revokeLink = new RevokePortalLinkService(clients, clients);

    private ClientData data(String name, String taxId) {
        return new ClientData(name, TaxId.optional(taxId), "maria@escritorio.com", "11999990000", null);
    }

    private ClientResponse cadastrar(String name) {
        return create.create(new CreateClientCommand(ORGANIZATION, ACTOR, data(name, null)));
    }

    @Test
    @DisplayName("create saves the client, publishes CLIENTE_CRIADO and returns the response")
    void create() {
        ClientResponse response = create.create(
                new CreateClientCommand(ORGANIZATION, ACTOR, data("Maria Souza", "529.982.247-25")));

        assertThat(response.id()).isNotBlank();
        assertThat(response.name()).isEqualTo("Maria Souza");
        assertThat(response.document()).isEqualTo("529.982.247-25");
        assertThat(response.portalActive()).isFalse();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(clients.all()).hasSize(1);
        assertThat(events.published).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(ClientEvent.CREATED);
            assertThat(event.organizationId()).isEqualTo(ORGANIZATION);
            assertThat(event.clientId().value()).isEqualTo(response.id());
            assertThat(event.description()).isEqualTo("Cliente criado: Maria Souza");
            assertThat(event.actor()).isEqualTo(ACTOR);
        });
    }

    @Test
    @DisplayName("a short name is rejected by the command itself, without saving or publishing")
    void shortName() {
        assertThatThrownBy(() -> new CreateClientCommand(ORGANIZATION, ACTOR, data("A", null)))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("Nome muito curto");
        assertThatThrownBy(() -> new UpdateClientCommand(ORGANIZATION, new ClientId("c1"), ACTOR, data("  ", null)))
                .isInstanceOf(InvalidDataException.class);

        assertThat(clients.all()).isEmpty();
        assertThat(events.published).isEmpty();
    }

    @Test
    @DisplayName("update replaces the data, publishes CLIENTE_ATUALIZADO and returns the response")
    void update() {
        ClientResponse created = cadastrar("Maria Souza");

        ClientResponse updated = update.update(new UpdateClientCommand(
                ORGANIZATION, new ClientId(created.id()), ACTOR, data("Maria Souza Lima", null)));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Maria Souza Lima");
        assertThat(events.published).hasSize(2);
        ClientEvent event = events.published.get(1);
        assertThat(event.type()).isEqualTo(ClientEvent.UPDATED);
        assertThat(event.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(event.clientId().value()).isEqualTo(created.id());
        assertThat(event.description()).isEqualTo("Cliente atualizado: Maria Souza Lima");
        assertThat(event.actor()).isEqualTo(ACTOR);
    }

    @Test
    @DisplayName("updating a client of another organization returns not found")
    void updateFromOtherOrganization() {
        ClientResponse created = cadastrar("Maria Souza");

        assertThatThrownBy(() -> update.update(new UpdateClientCommand(
                OTHER_ORGANIZATION, new ClientId(created.id()), ACTOR, data("Invasor", null))))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessage("Cliente não encontrado");
        assertThat(events.published).hasSize(1);
    }

    @Test
    @DisplayName("delete removes the client and publishes CLIENTE_EXCLUIDO")
    void delete() {
        ClientResponse created = cadastrar("Carlos Dias");

        delete.delete(new DeleteClientCommand(ORGANIZATION, new ClientId(created.id()), ACTOR));

        assertThat(clients.all()).isEmpty();
        assertThat(events.published).last().satisfies(event -> {
            assertThat(event.type()).isEqualTo(ClientEvent.DELETED);
            assertThat(event.description()).isEqualTo("Cliente excluído: Carlos Dias");
        });
    }

    @Test
    @DisplayName("deleting a client of another organization neither deletes nor publishes")
    void deleteFromOtherOrganization() {
        ClientResponse created = cadastrar("Carlos Dias");

        assertThatThrownBy(() -> delete.delete(
                new DeleteClientCommand(OTHER_ORGANIZATION, new ClientId(created.id()), ACTOR)))
                .isInstanceOf(ClientNotFoundException.class);

        assertThat(clients.all()).hasSize(1);
        assertThat(events.published).hasSize(1);
    }

    @Test
    @DisplayName("generate link creates a 64 character token valid for 90 days")
    void generateLink() {
        ClientResponse created = cadastrar("Helena Prado");

        PortalLinkResponse link = generateLink.generate(new GeneratePortalLinkCommand(ORGANIZATION, new ClientId(created.id())));

        assertThat(link.token()).hasSize(64);
        Client saved = clients.byPortalTokenHash(PortalToken.hash(link.token())).orElseThrow();
        assertThat(saved.portalLink().tokenHash()).isNotEqualTo(link.token());
        assertThat(saved.portalLink().expiresAt()).isEqualTo(Instant.parse("2027-02-09T12:00:00Z"));
        assertThat(saved.portalActiveAt(NOW)).isTrue();
    }

    @Test
    @DisplayName("a new link invalidates the previous one")
    void newLinkInvalidatesPrevious() {
        ClientResponse created = cadastrar("Helena Prado");
        GeneratePortalLinkCommand command = new GeneratePortalLinkCommand(ORGANIZATION, new ClientId(created.id()));

        String first = generateLink.generate(command).token();
        String second = generateLink.generate(command).token();

        assertThat(second).isNotEqualTo(first);
        assertThat(clients.byPortalTokenHash(PortalToken.hash(first))).isEmpty();
        assertThat(clients.byPortalTokenHash(PortalToken.hash(second))).isPresent();
    }

    @Test
    @DisplayName("revoke removes the link")
    void revokeLink() {
        ClientResponse created = cadastrar("Helena Prado");
        ClientId id = new ClientId(created.id());
        String token = generateLink.generate(new GeneratePortalLinkCommand(ORGANIZATION, id)).token();

        revokeLink.revoke(new RevokePortalLinkCommand(ORGANIZATION, id));

        assertThat(clients.byPortalTokenHash(PortalToken.hash(token))).isEmpty();
    }

    @Test
    @DisplayName("generating or revoking a link of a client of another organization returns not found")
    void linkFromOtherOrganization() {
        ClientResponse created = cadastrar("Helena Prado");
        ClientId id = new ClientId(created.id());

        assertThatThrownBy(() -> generateLink.generate(new GeneratePortalLinkCommand(OTHER_ORGANIZATION, id)))
                .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> revokeLink.revoke(new RevokePortalLinkCommand(OTHER_ORGANIZATION, id)))
                .isInstanceOf(ClientNotFoundException.class);
    }
}
