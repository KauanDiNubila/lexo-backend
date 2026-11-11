package app.lexo.adapter.out.persistence;

import app.lexo.common.domain.BaseEntity;
import app.lexo.domain.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "invoices",
        indexes = {
                @Index(name = "idx_invoice_org", columnList = "organizationId"),
                @Index(name = "idx_invoice_client", columnList = "clientId"),
                @Index(name = "idx_invoice_due", columnList = "dueDate"),
                @Index(name = "idx_invoice_status", columnList = "status")
        }
)
class InvoiceJpaEntity extends BaseEntity {

    @Column(nullable = false)
    String organizationId;

    @Column(nullable = false)
    String clientId;

    String caseId;

    @Column(nullable = false)
    String description;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Convert(converter = InvoiceStatusConverter.class)
    @Column(nullable = false)
    InvoiceStatus status;

    @Column(nullable = false)
    Instant dueDate;

    Instant paidAt;
}
