package app.lexo.application.service;

import app.lexo.application.port.in.DeleteClientCommand;
import app.lexo.application.port.in.DeleteClientUseCase;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.ClientEvent;
import app.lexo.application.port.out.DeleteClientPort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.domain.Client;
import app.lexo.domain.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteClientService implements DeleteClientUseCase {

    private final LoadClientPort loadClientPort;
    private final DeleteClientPort deleteClientPort;
    private final PublishEventPort publishEventPort;

    DeleteClientService(LoadClientPort loadClientPort, DeleteClientPort deleteClientPort,
                          PublishEventPort publishEventPort) {
        this.loadClientPort = loadClientPort;
        this.deleteClientPort = deleteClientPort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public void delete(DeleteClientCommand command) {
        Client client = loadClientPort.byId(command.organizationId(), command.clientId())
                .orElseThrow(ClientNotFoundException::new);
        deleteClientPort.delete(client);
        publishEventPort.publish(ClientEvent.deleted(client, command.actor()));
    }
}
