package app.lexo.application.service;

import app.lexo.application.port.in.ChangeInvoiceStatusCommand;
import app.lexo.application.port.in.ChangeInvoiceStatusUseCase;
import app.lexo.application.port.in.InvoiceResponse;
import app.lexo.application.port.out.LoadInvoicePort;
import app.lexo.application.port.out.SaveInvoicePort;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
class ChangeInvoiceStatusService implements ChangeInvoiceStatusUseCase {

    private final LoadInvoicePort loadInvoicePort;
    private final SaveInvoicePort saveInvoicePort;
    private final Clock clock;

    ChangeInvoiceStatusService(LoadInvoicePort loadInvoicePort, SaveInvoicePort saveInvoicePort, Clock clock) {
        this.loadInvoicePort = loadInvoicePort;
        this.saveInvoicePort = saveInvoicePort;
        this.clock = clock;
    }

    @Override
    public InvoiceResponse changeStatus(ChangeInvoiceStatusCommand command) {
        Invoice invoice = loadInvoicePort.byId(command.organizationId(), command.invoiceId())
                .orElseThrow(InvoiceNotFoundException::new);
        Instant now = clock.instant();
        invoice.changeStatus(command.status(), now);
        return InvoiceResponse.of(saveInvoicePort.save(invoice), now);
    }
}
