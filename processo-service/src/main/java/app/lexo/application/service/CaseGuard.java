package app.lexo.application.service;

import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

@Component
class CaseGuard {

    private final CaseExistsPort caseExistsPort;

    CaseGuard(CaseExistsPort caseExistsPort) {
        this.caseExistsPort = caseExistsPort;
    }

    void requireExists(OrganizationId organizationId, CaseId caseId) {
        if (!caseExistsPort.exists(organizationId, caseId)) {
            throw new CaseNotFoundException();
        }
    }
}
