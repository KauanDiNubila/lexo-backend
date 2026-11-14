package app.lexo.application.service;

import app.lexo.application.port.in.DeleteClientInvoicesCommand;
import app.lexo.application.port.in.DeleteClientInvoicesUseCase;
import app.lexo.application.port.out.DeleteClientInvoicesPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteClientInvoicesService implements DeleteClientInvoicesUseCase {

    private final DeleteClientInvoicesPort deleteClientInvoicesPort;

    DeleteClientInvoicesService(DeleteClientInvoicesPort deleteClientInvoicesPort) {
        this.deleteClientInvoicesPort = deleteClientInvoicesPort;
    }

    @Override
    public int deleteOfClient(DeleteClientInvoicesCommand command) {
        return deleteClientInvoicesPort.deleteOfClient(command.organizationId(), command.clientId());
    }
}
