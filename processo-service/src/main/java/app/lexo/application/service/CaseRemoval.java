package app.lexo.application.service;

import app.lexo.application.port.out.DeleteCasePort;
import app.lexo.application.port.out.DeleteDeadlinesOfCasePort;
import app.lexo.application.port.out.DeleteMovementsOfCasePort;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

@Component
class CaseRemoval {

    private final DeleteDeadlinesOfCasePort deleteDeadlinesPort;
    private final DeleteMovementsOfCasePort deleteMovementsPort;
    private final DeleteCasePort deleteCasePort;

    CaseRemoval(DeleteDeadlinesOfCasePort deleteDeadlinesPort, DeleteMovementsOfCasePort deleteMovementsPort,
                DeleteCasePort deleteCasePort) {
        this.deleteDeadlinesPort = deleteDeadlinesPort;
        this.deleteMovementsPort = deleteMovementsPort;
        this.deleteCasePort = deleteCasePort;
    }

    void remove(OrganizationId organizationId, CaseId caseId) {
        deleteDeadlinesPort.deleteOfCase(organizationId, caseId);
        deleteMovementsPort.deleteOfCase(organizationId, caseId);
        deleteCasePort.delete(organizationId, caseId);
    }
}
