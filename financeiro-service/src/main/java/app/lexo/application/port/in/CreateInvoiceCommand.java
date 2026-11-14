package app.lexo.application.port.in;

import app.lexo.domain.InvoiceData;
import app.lexo.domain.OrganizationId;

public record CreateInvoiceCommand(OrganizationId organizationId, InvoiceData data) {

    public CreateInvoiceCommand {
        InvoiceInput.requireValid(data);
    }
}
