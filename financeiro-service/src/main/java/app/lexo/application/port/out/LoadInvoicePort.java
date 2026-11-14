package app.lexo.application.port.out;

import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;
import java.util.Optional;

public interface LoadInvoicePort {

    Optional<Invoice> byId(OrganizationId organizationId, InvoiceId invoiceId);
}
