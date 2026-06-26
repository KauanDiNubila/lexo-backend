package app.lexo.domain;

import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.domain.enums.DeadlineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** Prazo/compromisso da agenda. */
@Entity
@Table(
        name = "deadlines",
        indexes = {
                @Index(name = "idx_deadline_org", columnList = "organizationId"),
                @Index(name = "idx_deadline_case", columnList = "caseId"),
                @Index(name = "idx_deadline_date", columnList = "date"),
                @Index(name = "idx_deadline_status", columnList = "status")
        }
)
public class Deadline extends BaseEntity {

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeadlineType type = DeadlineType.PRAZO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeadlineStatus status = DeadlineStatus.PENDENTE;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant date;

    private Instant notifiedAt;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public DeadlineType getType() {
        return type;
    }

    public void setType(DeadlineType type) {
        this.type = type;
    }

    public DeadlineStatus getStatus() {
        return status;
    }

    public void setStatus(DeadlineStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public Instant getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Instant notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}
