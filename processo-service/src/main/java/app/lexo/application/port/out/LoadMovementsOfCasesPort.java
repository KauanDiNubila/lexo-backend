package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.OrganizationId;

import java.util.Collection;
import java.util.List;

public interface LoadMovementsOfCasesPort {

    List<CaseMovement> ofCases(OrganizationId organizationId, Collection<CaseId> caseIds);
}
