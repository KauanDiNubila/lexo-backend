package app.lexo.application.service;

import app.lexo.application.port.in.DeleteInvoiceCommand;
import app.lexo.application.port.in.DeleteInvoiceUseCase;
import app.lexo.application.port.out.DeleteInvoicePort;
import app.lexo.domain.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteInvoiceService implements DeleteInvoiceUseCase {

    private final DeleteInvoicePort deleteInvoicePort;

    DeleteInvoiceService(DeleteInvoicePort deleteInvoicePort) {
        this.deleteInvoicePort = deleteInvoicePort;
    }

    @Override
    public void delete(DeleteInvoiceCommand command) {
        if (!deleteInvoicePort.delete(command.organizationId(), command.invoiceId())) {
            throw new InvoiceNotFoundException();
        }
    }
}
