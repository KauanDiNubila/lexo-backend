package app.lexo.application.service;

import app.lexo.application.port.in.GetClientQuery;
import app.lexo.application.port.in.ClientOption;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.in.ClientExistsQuery;
import app.lexo.application.port.in.ListClientsQuery;
import app.lexo.application.port.in.ListClientOptionsQuery;
import app.lexo.common.web.PageParams;
import app.lexo.application.port.in.ClientSummary;
import app.lexo.application.port.in.ClientSummaryQuery;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.CountClientsPort;
import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.application.port.out.ListClientsPort;
import app.lexo.application.port.out.ListClientOptionsPort;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
class ClientQueryService implements GetClientQuery, ListClientsQuery, ListClientOptionsQuery,
        ClientSummaryQuery, ClientExistsQuery {

    private final LoadClientPort loadClientPort;
    private final ListClientsPort listClientsPort;
    private final ListClientOptionsPort listOptionsPort;
    private final CountClientsPort countClientsPort;
    private final ClientExistsPort clientExistsPort;
    private final Clock clock;

    ClientQueryService(LoadClientPort loadClientPort, ListClientsPort listClientsPort,
                        ListClientOptionsPort listOptionsPort, CountClientsPort countClientsPort,
                        ClientExistsPort clientExistsPort, Clock clock) {
        this.loadClientPort = loadClientPort;
        this.listClientsPort = listClientsPort;
        this.listOptionsPort = listOptionsPort;
        this.countClientsPort = countClientsPort;
        this.clientExistsPort = clientExistsPort;
        this.clock = clock;
    }

    @Override
    public ClientResponse get(OrganizationId organizationId, ClientId clientId) {
        Client client = loadClientPort.byId(organizationId, clientId)
                .orElseThrow(ClientNotFoundException::new);
        return ClientResponse.of(client, clock.instant());
    }

    @Override
    public Pagina<ClientResponse> list(OrganizationId organizationId, String search, PageParams pageParams) {
        Pagina<Client> page = listClientsPort.list(organizationId, search, pageParams);
        Instant now = clock.instant();
        List<ClientResponse> content = page.content().stream().map(c -> ClientResponse.of(c, now)).toList();
        return new Pagina<>(content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    @Override
    public List<ClientOption> listOptions(OrganizationId organizationId) {
        return listOptionsPort.options(organizationId);
    }

    @Override
    public ClientSummary summarize(OrganizationId organizationId) {
        return new ClientSummary(countClientsPort.count(organizationId));
    }

    @Override
    public boolean exists(OrganizationId organizationId, ClientId clientId) {
        return clientExistsPort.exists(organizationId, clientId);
    }
}
