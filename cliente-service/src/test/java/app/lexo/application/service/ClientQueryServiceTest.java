package app.lexo.application.service;

import app.lexo.application.port.in.ClientOption;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.common.web.SortOrder;
import app.lexo.common.web.PageParams;
import app.lexo.application.port.in.ClientSummary;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static app.lexo.application.service.InMemoryClients.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClientQueryService")
class ClientQueryServiceTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");
    private static final OrganizationId OTHER_ORGANIZATION = new OrganizationId("escritorio-2");

    private final InMemoryClients clients = new InMemoryClients();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private Client save(OrganizationId organization, String name) {
        return clients.save(Client.register(organization, new ClientData(name, null, null, null, null)));
    }

    @Test
    @DisplayName("get returns the client of the organization and not found for another one")
    void get() {
        Client maria = save(ORGANIZATION, "Maria Souza");
        ClientQueryService service = new ClientQueryService(clients, null, null, null, null, clock);

        ClientResponse response = service.get(ORGANIZATION, maria.id());

        assertThat(response.name()).isEqualTo("Maria Souza");
        assertThatThrownBy(() -> service.get(OTHER_ORGANIZATION, maria.id()))
                .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> service.get(ORGANIZATION, new ClientId("inexistente")))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("list converts the domain page into responses, keeping the totals")
    void list() {
        Client ana = save(ORGANIZATION, "Ana Paula");
        PageParams pageParams = new PageParams(0, 20, List.of(new SortOrder("name", true)));
        ClientQueryService service = new ClientQueryService(clients,
                (organization, search, request) -> new PageResult<>(List.of(ana), 0, 20, 1, 1),
                null, null, null, clock);

        PageResult<ClientResponse> page = service.list(ORGANIZATION, "ana", pageParams);

        assertThat(page.content()).extracting(ClientResponse::name).containsExactly("Ana Paula");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.page()).isZero();
    }

    @Test
    @DisplayName("options, summary and exists delegate to the outbound ports")
    void delegations() {
        ClientQueryService service = new ClientQueryService(clients, null,
                organization -> List.of(new ClientOption("c1", "Ana")),
                organization -> 7L,
                (organization, id) -> id.value().equals("c1"),
                clock);

        assertThat(service.listOptions(ORGANIZATION)).containsExactly(new ClientOption("c1", "Ana"));
        assertThat(service.summarize(ORGANIZATION)).isEqualTo(new ClientSummary(7));
        assertThat(service.exists(ORGANIZATION, new ClientId("c1"))).isTrue();
        assertThat(service.exists(ORGANIZATION, new ClientId("c2"))).isFalse();
    }
}
