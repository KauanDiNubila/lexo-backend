package app.lexo.application.port.out;

import app.lexo.domain.Invoice;

public interface DeleteInvoicePort {

    void delete(Invoice invoice);
}
