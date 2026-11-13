package app.lexo.application.port.in;

import app.lexo.domain.CaseContext;
import app.lexo.domain.OrganizationId;

import java.util.Optional;

public record DraftPetitionCommand(OrganizationId organizationId, String instruction,
                                   Optional<CaseContext> caseContext) {
}
