package app.lexo.adapter.out.persistence;

import app.lexo.domain.CaseId;
import app.lexo.domain.ClientId;
import app.lexo.domain.Invoice;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.InvoiceId;
import app.lexo.domain.OrganizationId;

final class InvoiceMapper {

    private InvoiceMapper() {
    }

    static Invoice toDomain(InvoiceJpaEntity entity) {
        InvoiceData data = new InvoiceData(new ClientId(entity.clientId), CaseId.optional(entity.caseId),
                entity.description, entity.amount, entity.status, entity.dueDate);
        return Invoice.restore(new InvoiceId(entity.getId()), new OrganizationId(entity.organizationId), data,
                entity.paidAt);
    }

    static void copyTo(Invoice invoice, InvoiceJpaEntity entity) {
        InvoiceData data = invoice.data();
        entity.organizationId = invoice.organizationId().value();
        entity.clientId = data.clientId().value();
        entity.caseId = data.caseId() == null ? null : data.caseId().value();
        entity.description = data.description();
        entity.amount = data.amount();
        entity.status = data.status();
        entity.dueDate = data.dueDate();
        entity.paidAt = invoice.paidAt();
    }
}
