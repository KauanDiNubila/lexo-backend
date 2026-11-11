package app.lexo.application.service;

import app.lexo.application.port.in.DeleteInvoiceCommand;
import app.lexo.application.port.in.DeleteInvoiceUseCase;
import app.lexo.application.port.out.DeleteInvoicePort;
import app.lexo.application.port.out.LoadInvoicePort;
import app.lexo.domain.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteInvoiceService implements DeleteInvoiceUseCase {

    private final LoadInvoicePort loadInvoicePort;
    private final DeleteInvoicePort deleteInvoicePort;

    DeleteInvoiceService(LoadInvoicePort loadInvoicePort, DeleteInvoicePort deleteInvoicePort) {
        this.loadInvoicePort = loadInvoicePort;
        this.deleteInvoicePort = deleteInvoicePort;
    }

    @Override
    public void delete(DeleteInvoiceCommand command) {
        deleteInvoicePort.delete(loadInvoicePort.byId(command.organizationId(), command.invoiceId())
                .orElseThrow(InvoiceNotFoundException::new));
    }
}
