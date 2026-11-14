package app.lexo.application.port.out;

import app.lexo.domain.Invoice;
import app.lexo.domain.OrganizationId;
import java.time.Instant;
import java.util.List;

public interface LoadInvoicesDuePort {

    List<Invoice> dueBetween(OrganizationId organizationId, Instant start, Instant end);
}
