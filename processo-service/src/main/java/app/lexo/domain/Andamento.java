package app.lexo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "andamentos",
        indexes = {
                @Index(name = "idx_andamento_org", columnList = "organizationId"),
                @Index(name = "idx_andamento_case", columnList = "caseId"),
                @Index(name = "idx_andamento_date", columnList = "date")
        }
)
public class Andamento extends BaseEntity {

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant date;

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
}
