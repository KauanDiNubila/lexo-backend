package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.in.ClientOption;
import app.lexo.common.web.SortOrder;
import app.lexo.common.web.PageParams;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.CountClientsPort;
import app.lexo.application.port.out.DeleteClientPort;
import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.application.port.out.ListClientsPort;
import app.lexo.application.port.out.ListClientOptionsPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.TaxId;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.PortalToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Client persistence adapter (integration, Postgres)")
class ClientPersistenceAdapterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SaveClientPort save;

    @Autowired
    private LoadClientPort load;

    @Autowired
    private DeleteClientPort delete;

    @Autowired
    private ListClientsPort list;

    @Autowired
    private ListClientOptionsPort options;

    @Autowired
    private CountClientsPort count;

    @Autowired
    private ClientExistsPort exists;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private Client saveClient(OrganizationId organization, String name, String taxId, String email) {
        return save.save(Client.register(organization,
                new ClientData(name, TaxId.optional(taxId), email, null, null)));
    }

    @Test
    @DisplayName("saving a new client generates an id and creation date and loads back with all data")
    void saveNew() {
        OrganizationId organization = newOrganization();

        Client saved = save.save(Client.register(organization,
                new ClientData("Maria Souza", TaxId.of("529.982.247-25"), "maria@x.com", "11999990000", "obs")));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        Client read = load.byId(organization, saved.id()).orElseThrow();
        assertThat(read.data()).isEqualTo(saved.data());
        assertThat(read.data().taxId().text()).isEqualTo("529.982.247-25");
        assertThat(read.organizationId()).isEqualTo(organization);
    }

    @Test
    @DisplayName("saving an existing client updates the same row and keeps the creation date")
    void saveExisting() {
        OrganizationId organization = newOrganization();
        Client created = load.byId(organization, saveClient(organization, "Maria Souza", null, null).id()).orElseThrow();

        created.update(new ClientData("Maria Souza Lima", null, "novo@x.com", null, null));
        Client updated = save.save(created);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(load.byId(organization, created.id()).orElseThrow().name()).isEqualTo("Maria Souza Lima");
        assertThat(count.count(organization)).isEqualTo(1);
    }

    @Test
    @DisplayName("loading by id respects the organization")
    void loadRespectsOrganization() {
        OrganizationId organization = newOrganization();
        Client saved = saveClient(organization, "Maria Souza", null, null);

        assertThat(load.byId(newOrganization(), saved.id())).isEmpty();
        assertThat(load.byId(organization, new ClientId("inexistente"))).isEmpty();
        assertThat(exists.exists(organization, saved.id())).isTrue();
        assertThat(exists.exists(newOrganization(), saved.id())).isFalse();
    }

    @Test
    @DisplayName("the portal link is saved, found by token and removed on revoke")
    void portalLink() {
        OrganizationId organization = newOrganization();
        Client client = saveClient(organization, "Helena Prado", null, null);
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        client.generatePortalLink(PortalToken.hash(token), now, 90);
        save.save(client);

        Client byToken = load.byPortalTokenHash(PortalToken.hash(token)).orElseThrow();
        assertThat(byToken.id()).isEqualTo(client.id());
        assertThat(byToken.portalActiveAt(now)).isTrue();

        byToken.revokePortalLink();
        save.save(byToken);
        assertThat(load.byPortalTokenHash(PortalToken.hash(token))).isEmpty();
    }

    @Test
    @DisplayName("a legacy document, outside today's rule, is still read from the database")
    void legacyDocumentIsStillRead() {
        OrganizationId organization = newOrganization();
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into clients (id, organization_id, name, document, created_at, updated_at) "
                + "values (?, ?, ?, ?, now(), now())", id, organization.value(), "A", "documento antigo");

        Client read = load.byId(organization, new ClientId(id)).orElseThrow();

        assertThat(read.name()).isEqualTo("A");
        assertThat(read.data().taxId().text()).isEqualTo("documento antigo");
    }

    @Test
    @DisplayName("list filters by organization and text, sorts and paginates")
    void list() {
        OrganizationId organization = newOrganization();
        saveClient(organization, "Carla Mendes", "529.982.247-25", "carla@x.com");
        saveClient(organization, "Ana Paula", null, "ANA@LOJA.COM");
        saveClient(organization, "Bruno Reis", null, null);
        saveClient(newOrganization(), "Outro Escritorio", null, null);

        PageResult<Client> all = list.list(organization, null, new PageParams(0, 20, List.of(new SortOrder("name", true))));
        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.content()).extracting(Client::name).containsExactly("Ana Paula", "Bruno Reis", "Carla Mendes");

        PageResult<Client> descending = list.list(organization, "  ", new PageParams(0, 20, List.of(new SortOrder("name", false))));
        assertThat(descending.content().get(0).name()).isEqualTo("Carla Mendes");

        PageResult<Client> second = list.list(organization, null, new PageParams(1, 2, List.of(new SortOrder("name", true))));
        assertThat(second.content()).hasSize(1);
        assertThat(second.totalPages()).isEqualTo(2);

        assertThat(list.list(organization, "ana@loja", new PageParams(0, 20, List.of())).totalElements()).isEqualTo(1);
        assertThat(list.list(organization, "529.982", new PageParams(0, 20, List.of())).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("options come out alphabetically, only for the organization")
    void options() {
        OrganizationId organization = newOrganization();
        Client zeca = saveClient(organization, "Zeca Alves", null, null);
        Client ana = saveClient(organization, "Ana Paula", null, null);
        saveClient(newOrganization(), "Fora", null, null);

        List<ClientOption> list = options.options(organization);

        assertThat(list).containsExactly(
                new ClientOption(ana.id().value(), "Ana Paula"), new ClientOption(zeca.id().value(), "Zeca Alves"));
    }

    @Test
    @DisplayName("delete and save only reach clients of their own organization")
    void organizationScope() {
        OrganizationId owner = newOrganization();
        OrganizationId intruder = newOrganization();
        Client client = saveClient(owner, "Carlos Dias", null, null);
        Client asIntruder = Client.restore(client.id(), intruder, client.data(), null, client.createdAt());

        delete.delete(asIntruder);

        assertThat(load.byId(owner, client.id())).isPresent();
        assertThatThrownBy(() -> save.save(asIntruder)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("delete removes the client")
    void delete() {
        OrganizationId organization = newOrganization();
        Client client = saveClient(organization, "Carlos Dias", null, null);

        delete.delete(client);

        assertThat(load.byId(organization, client.id())).isEmpty();
        assertThat(count.count(organization)).isZero();
    }
}
