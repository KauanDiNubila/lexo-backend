package app.lexo.adapter.out.finance;

import app.lexo.application.port.out.LoadClientFeesPort;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ClientFeesAdapter implements LoadClientFeesPort {

    private final FinancePortalClient client;

    ClientFeesAdapter(FinancePortalClient client) {
        this.client = client;
    }

    @Override
    public List<HonorarioPortal> load(OrganizationId organizationId, ClientId clientId) {
        return client.feesOfClient(clientId.value(), organizationId.value());
    }
}
