package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.OrganizationId;

import java.util.Collection;
import java.util.List;

public interface LoadDeadlinesOfCasesPort {

    List<Deadline> ofCases(OrganizationId organizationId, Collection<CaseId> caseIds);
}
