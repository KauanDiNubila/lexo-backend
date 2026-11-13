package app.lexo.application.service;

import app.lexo.application.port.in.UpdateClientCommand;
import app.lexo.application.port.in.UpdateClientUseCase;
import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.out.ClientEvent;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.PublishEventPort;
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
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    UpdateClientService(LoadClientPort loadClientPort, SaveClientPort saveClientPort,
                        PublishEventPort publishEventPort, Clock clock) {
        this.loadClientPort = loadClientPort;
        this.saveClientPort = saveClientPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public ClientResponse update(UpdateClientCommand command) {
        Client client = loadClientPort.byId(command.organizationId(), command.clientId())
                .orElseThrow(ClientNotFoundException::new);
        client.update(command.data());
        Client saved = saveClientPort.save(client);
        publishEventPort.publish(ClientEvent.updated(saved, command.actor()));
        return ClientResponse.of(saved, clock.instant());
    }
}
