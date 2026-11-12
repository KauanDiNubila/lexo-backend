package app.lexo.adapter.out.client;

import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.application.port.out.LoadUserContactPort;
import app.lexo.application.port.out.UserContact;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.client.ReferenciaGateway;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class TemporaryReferenceAdapter implements ClientExistsPort, UserExistsPort, LoadUserContactPort {

    private final ReferenciaGateway gateway;

    TemporaryReferenceAdapter(ReferenciaGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean exists(OrganizationId organizationId, ClientId clientId) {
        return gateway.clienteExiste(clientId.value(), organizationId.value());
    }

    @Override
    public boolean exists(OrganizationId organizationId, UserId userId) {
        return gateway.usuarioExiste(userId.value(), organizationId.value());
    }

    @Override
    public Optional<UserContact> byId(OrganizationId organizationId, UserId userId) {
        return gateway.buscarUsuario(userId.value(), organizationId.value())
                .map(user -> new UserContact(user.name(), user.email()));
    }
}
