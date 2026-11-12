package app.lexo.application.port.out;

import app.lexo.domain.CaseStatus;
import app.lexo.domain.OrganizationId;

import java.util.Map;

public interface CountCasesByStatusPort {

    Map<CaseStatus, Long> countByStatus(OrganizationId organizationId);
}
