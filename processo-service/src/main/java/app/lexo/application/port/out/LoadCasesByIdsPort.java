package app.lexo.application.port.out;

import app.lexo.domain.Case;
import app.lexo.domain.CaseId;

import java.util.Collection;
import java.util.List;

public interface LoadCasesByIdsPort {

    List<Case> byIds(Collection<CaseId> caseIds);
}
