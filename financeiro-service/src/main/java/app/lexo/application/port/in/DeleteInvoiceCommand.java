package app.lexo.application.port.in;

import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;

public record DeleteInvoiceCommand(OrganizationId organizationId, InvoiceId invoiceId) {
}
