package app.lexo.application.port.out;

import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;

public interface DeleteInvoicePort {

    boolean delete(OrganizationId organizationId, InvoiceId invoiceId);
}
