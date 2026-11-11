package app.lexo.application.port.out;

import app.lexo.domain.InvoiceStatus;
import app.lexo.domain.OrganizationId;
import java.math.BigDecimal;
import java.util.Set;

public interface SumInvoicesPort {

    BigDecimal sumByStatus(OrganizationId organizationId, Set<InvoiceStatus> statuses);
}
