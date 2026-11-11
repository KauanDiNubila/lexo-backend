package app.lexo.application.service;

import app.lexo.application.port.in.CreateInvoiceCommand;
import app.lexo.application.port.in.CreateInvoiceUseCase;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.out.SaveInvoicePort;
import app.lexo.domain.Invoice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class CreateInvoiceService implements CreateInvoiceUseCase {

    private final InvoiceReferences references;
    private final SaveInvoicePort saveInvoicePort;
    private final Clock clock;

    CreateInvoiceService(InvoiceReferences references, SaveInvoicePort saveInvoicePort, Clock clock) {
        this.references = references;
        this.saveInvoicePort = saveInvoicePort;
        this.clock = clock;
    }

    @Override
    public InvoiceResponse create(CreateInvoiceCommand command) {
        references.requireExist(command.organizationId(), command.data());
        Invoice invoice = Invoice.issue(command.organizationId(), command.data(), clock.instant());
        return InvoiceResponse.of(saveInvoicePort.save(invoice));
    }
}
