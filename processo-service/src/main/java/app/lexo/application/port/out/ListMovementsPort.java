package app.lexo.application.port.out;

import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface ListMovementsPort {

    List<CaseMovement> ofCase(OrganizationId organizationId, CaseId caseId);
}
