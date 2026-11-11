package app.lexo.application.service;

import app.lexo.application.port.out.DeleteInvoicePort;
import app.lexo.application.port.out.LoadInvoicePort;
import app.lexo.application.port.out.SaveInvoicePort;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryInvoices implements SaveInvoicePort, LoadInvoicePort, DeleteInvoicePort {

    private final Map<String, Invoice> invoices = new LinkedHashMap<>();
    private int sequence;

    @Override
    public Invoice save(Invoice invoice) {
        Invoice saved = invoice.id() == null
                ? Invoice.restore(new InvoiceId("i" + (++sequence)), invoice.organizationId(), invoice.data(),
                        invoice.paidAt())
                : invoice;
        invoices.put(saved.id().value(), saved);
        return saved;
    }

    @Override
    public Optional<Invoice> byId(OrganizationId organizationId, InvoiceId invoiceId) {
        return Optional.ofNullable(invoices.get(invoiceId.value()))
                .filter(invoice -> invoice.organizationId().equals(organizationId));
    }

    @Override
    public boolean delete(OrganizationId organizationId, InvoiceId invoiceId) {
        return byId(organizationId, invoiceId).map(invoice -> invoices.remove(invoiceId.value()) != null).orElse(false);
    }

    List<Invoice> all() {
        return new ArrayList<>(invoices.values());
    }
}
