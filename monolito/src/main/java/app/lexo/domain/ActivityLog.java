package app.lexo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Historico de atividades por processo. */
@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_org", columnList = "organizationId"),
                @Index(name = "idx_activity_case", columnList = "caseId"),
                @Index(name = "idx_activity_created", columnList = "createdAt")
        }
)
public class ActivityLog extends BaseEntity {

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false)
    private String caseId;

    private String userId;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String action;

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
}
