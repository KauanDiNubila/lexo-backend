package app.lexo.application.service;

import app.lexo.application.port.in.RevokePortalLinkCommand;
import app.lexo.application.port.in.RevokePortalLinkUseCase;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.domain.Client;
import app.lexo.domain.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RevokePortalLinkService implements RevokePortalLinkUseCase {

    private final LoadClientPort loadClientPort;
    private final SaveClientPort saveClientPort;

    RevokePortalLinkService(LoadClientPort loadClientPort, SaveClientPort saveClientPort) {
        this.loadClientPort = loadClientPort;
        this.saveClientPort = saveClientPort;
    }

    @Override
    public void revoke(RevokePortalLinkCommand command) {
        Client client = loadClientPort.byId(command.organizationId(), command.clientId())
                .orElseThrow(ClientNotFoundException::new);
        client.revokePortalLink();
        saveClientPort.save(client);
    }
}
