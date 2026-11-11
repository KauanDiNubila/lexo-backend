package app.lexo.adapter.out.client;

import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.client.ReferenciaGateway;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

@Component
class ClientExistsAdapter implements ClientExistsPort {

    private final ReferenciaGateway gateway;

    ClientExistsAdapter(ReferenciaGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean exists(OrganizationId organizationId, ClientId clientId) {
        return gateway.clienteExiste(clientId.value(), organizationId.value());
    }
}
