package app.lexo.application.port.out;

import app.lexo.application.port.in.CaseOption;
import app.lexo.domain.OrganizationId;

import java.util.List;

public interface ListCaseOptionsPort {

    List<CaseOption> options(OrganizationId organizationId);
}
