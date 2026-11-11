package app.lexo.adapter.out.cases;

import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ClientCasesAdapter implements LoadClientCasesPort {

    private final CasePortalClient client;

    ClientCasesAdapter(CasePortalClient client) {
        this.client = client;
    }

    @Override
    public List<ProcessoPortal> load(OrganizationId organizationId, ClientId clientId) {
        return client.casesOfClient(clientId.value(), organizationId.value());
    }
}
