package app.lexo.application.port.in;

import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;

public record UpdateInvoiceCommand(OrganizationId organizationId, InvoiceId invoiceId, InvoiceData data) {

    public UpdateInvoiceCommand {
        InvoiceInput.requireValid(data);
    }
}
