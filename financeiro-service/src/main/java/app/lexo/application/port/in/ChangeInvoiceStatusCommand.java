package app.lexo.application.port.in;

import app.lexo.domain.InvoiceId;
import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;

public record ChangeInvoiceStatusCommand(OrganizationId organizationId, InvoiceId invoiceId, InvoiceStatus status) {
}
