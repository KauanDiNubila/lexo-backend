package app.lexo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Log de auditoria de acoes sensiveis (visivel a administradores). */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_org", columnList = "organizationId"),
                @Index(name = "idx_audit_created", columnList = "createdAt")
        }
)
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private String organizationId;

    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String action;

    private String entityType;
    private String entityId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
