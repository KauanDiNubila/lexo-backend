package app.lexo.domain;

import java.time.Instant;

public class Invoice {

    private final InvoiceId id;
    private final OrganizationId organizationId;
    private InvoiceData data;
    private Instant paidAt;

    private Invoice(InvoiceId id, OrganizationId organizationId, InvoiceData data, Instant paidAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.data = data;
        this.paidAt = paidAt;
    }

    public static Invoice issue(OrganizationId organizationId, InvoiceData data, Instant now) {
        return new Invoice(null, organizationId, data, data.status() == InvoiceStatus.PAID ? now : null);
    }

    public static Invoice restore(InvoiceId id, OrganizationId organizationId, InvoiceData data, Instant paidAt) {
        return new Invoice(id, organizationId, data, paidAt);
    }

    public void update(InvoiceData newData, Instant now) {
        moveTo(newData.status(), now);
        this.data = newData;
    }

    public void changeStatus(InvoiceStatus newStatus, Instant now) {
        moveTo(newStatus, now);
        this.data = data.withStatus(newStatus);
    }

    public InvoiceStatus statusAt(Instant now) {
        boolean pastDue = data.status() == InvoiceStatus.PENDING && data.dueDate().isBefore(now);
        return pastDue ? InvoiceStatus.OVERDUE : data.status();
    }

    private void moveTo(InvoiceStatus newStatus, Instant now) {
        if (newStatus != InvoiceStatus.PAID) {
            this.paidAt = null;
        } else if (data.status() != InvoiceStatus.PAID) {
            this.paidAt = now;
        }
    }

    public InvoiceId id() {
        return id;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public InvoiceData data() {
        return data;
    }

    public Instant paidAt() {
        return paidAt;
    }
}
