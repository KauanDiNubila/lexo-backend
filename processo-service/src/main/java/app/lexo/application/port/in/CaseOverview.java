package app.lexo.application.port.in;

import java.util.List;

public record CaseOverview(CaseResponse caseData, List<DeadlineResponse> deadlines, List<MovementResponse> movements) {
}
