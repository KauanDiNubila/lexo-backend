package app.lexo.application.service;

import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.in.UpdateInvoiceCommand;
import app.lexo.application.port.in.UpdateInvoiceUseCase;
import app.lexo.application.port.out.LoadInvoicePort;
import app.lexo.application.port.out.SaveInvoicePort;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
class UpdateInvoiceService implements UpdateInvoiceUseCase {

    private final InvoiceReferences references;
    private final LoadInvoicePort loadInvoicePort;
    private final SaveInvoicePort saveInvoicePort;
    private final Clock clock;

    UpdateInvoiceService(InvoiceReferences references, LoadInvoicePort loadInvoicePort,
                         SaveInvoicePort saveInvoicePort, Clock clock) {
        this.references = references;
        this.loadInvoicePort = loadInvoicePort;
        this.saveInvoicePort = saveInvoicePort;
        this.clock = clock;
    }

    @Override
    public InvoiceResponse update(UpdateInvoiceCommand command) {
        references.requireExist(command.organizationId(), command.data());
        Invoice invoice = loadInvoicePort.byId(command.organizationId(), command.invoiceId())
                .orElseThrow(InvoiceNotFoundException::new);
        invoice.update(command.data(), clock.instant());
        return InvoiceResponse.of(saveInvoicePort.save(invoice));
    }
}
