package app.lexo.application.port.in;

import app.lexo.domain.CaseContext;
import app.lexo.domain.Deadline;
import app.lexo.domain.OrganizationId;

import java.util.List;

public record SummarizeCaseCommand(OrganizationId organizationId, CaseContext caseContext,
                                   List<Deadline> deadlines) {

    public SummarizeCaseCommand {
        deadlines = deadlines == null ? List.of() : List.copyOf(deadlines);
    }
}
