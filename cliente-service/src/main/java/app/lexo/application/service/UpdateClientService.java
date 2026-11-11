package app.lexo.application.service;

import app.lexo.application.port.in.UpdateClientCommand;
import app.lexo.application.port.in.UpdateClientUseCase;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.domain.Client;
import app.lexo.domain.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class UpdateClientService implements UpdateClientUseCase {

    private final LoadClientPort loadClientPort;
    private final SaveClientPort saveClientPort;
    private final Clock clock;

    UpdateClientService(LoadClientPort loadClientPort, SaveClientPort saveClientPort,
                            Clock clock) {
        this.loadClientPort = loadClientPort;
        this.saveClientPort = saveClientPort;
        this.clock = clock;
    }

    @Override
    public ClientResponse update(UpdateClientCommand command) {
        Client client = loadClientPort.byId(command.organizationId(), command.clientId())
                .orElseThrow(ClientNotFoundException::new);
        client.update(command.data());
        return ClientResponse.of(saveClientPort.save(client), clock.instant());
    }
}
