package app.lexo.adapter.out.finance;

import app.lexo.application.port.out.LoadClientFeesPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ClientFeesAdapter implements LoadClientFeesPort {

    private static final Logger log = LoggerFactory.getLogger(ClientFeesAdapter.class);

    private final FinancePortalClient client;

    ClientFeesAdapter(FinancePortalClient client) {
        this.client = client;
    }

    @Override
    public List<PortalFee> load(OrganizationId organizationId, ClientId clientId) {
        try {
            return client.feesOfClient(clientId.value(), organizationId.value());
        } catch (RuntimeException e) {
            log.warn("Portal: failed to load fees ({})", e.toString());
            throw new ServiceUnavailableException("Não foi possível carregar o portal agora. Tente novamente em instantes.");
        }
    }
}
