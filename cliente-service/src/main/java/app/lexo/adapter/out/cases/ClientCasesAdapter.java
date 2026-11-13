package app.lexo.adapter.out.cases;

import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ClientCasesAdapter implements LoadClientCasesPort {

    private static final Logger log = LoggerFactory.getLogger(ClientCasesAdapter.class);

    private final CasePortalClient client;

    ClientCasesAdapter(CasePortalClient client) {
        this.client = client;
    }

    @Override
    public List<PortalCase> load(OrganizationId organizationId, ClientId clientId) {
        try {
            return client.casesOfClient(clientId.value(), organizationId.value());
        } catch (RuntimeException e) {
            log.warn("Portal: failed to load cases ({})", e.toString());
            throw new ServiceUnavailableException("Não foi possível carregar o portal agora. Tente novamente em instantes.");
        }
    }
}
