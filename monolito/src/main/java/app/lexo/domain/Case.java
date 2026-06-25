package app.lexo.domain;

import app.lexo.domain.enums.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Processo judicial. Tabela "cases" (CASE e palavra reservada em SQL). */
@Entity
@Table(
        name = "cases",
        uniqueConstraints = @UniqueConstraint(name = "uq_case_org_number", columnNames = {"organizationId", "number"}),
        indexes = {
                @Index(name = "idx_case_org", columnList = "organizationId"),
                @Index(name = "idx_case_client", columnList = "clientId"),
                @Index(name = "idx_case_responsavel", columnList = "responsavelId"),
                @Index(name = "idx_case_status", columnList = "status")
        }
)
public class Case extends BaseEntity {

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String number;

    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.ATIVO;

    @Column(columnDefinition = "text")
    private String description;

    private String responsavelId;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(String responsavelId) {
        this.responsavelId = responsavelId;
    }
}
