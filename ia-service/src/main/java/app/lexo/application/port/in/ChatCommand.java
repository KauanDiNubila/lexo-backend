package app.lexo.application.port.in;

import app.lexo.domain.CaseContext;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.OrganizationId;

import java.util.List;
import java.util.Optional;

public record ChatCommand(OrganizationId organizationId, List<ChatMessage> messages,
                          Optional<CaseContext> caseContext) {

    public ChatCommand {
        messages = List.copyOf(messages);
    }
}
