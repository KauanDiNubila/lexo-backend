package app.lexo.application.port.out;

import app.lexo.domain.Invoice;

public interface SaveInvoicePort {

    Invoice save(Invoice invoice);
}
