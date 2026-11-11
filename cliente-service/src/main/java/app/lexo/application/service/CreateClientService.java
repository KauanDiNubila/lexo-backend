package app.lexo.application.service;

import app.lexo.application.port.in.ClientResponse;
import app.lexo.application.port.in.CreateClientCommand;
import app.lexo.application.port.in.CreateClientUseCase;
import app.lexo.application.port.out.ClientEvent;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.domain.Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class CreateClientService implements CreateClientUseCase {

    private final SaveClientPort saveClientPort;
    private final PublishEventPort publishEventPort;
    private final Clock clock;

    CreateClientService(SaveClientPort saveClientPort, PublishEventPort publishEventPort, Clock clock) {
        this.saveClientPort = saveClientPort;
        this.publishEventPort = publishEventPort;
        this.clock = clock;
    }

    @Override
    public ClientResponse create(CreateClientCommand command) {
        Client saved = saveClientPort.save(Client.register(command.organizationId(), command.data()));
        publishEventPort.publish(ClientEvent.created(saved, command.actor()));
        return ClientResponse.of(saved, clock.instant());
    }
}
