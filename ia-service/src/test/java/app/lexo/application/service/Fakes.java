package app.lexo.application.service;

import app.lexo.application.AiSettings;
import app.lexo.application.port.out.RateLimitPort;
import app.lexo.application.port.out.TextGenerationPort;
import app.lexo.domain.ChatMessage;
import app.lexo.domain.OrganizationId;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Fakes {

    static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");
    static final AiSettings SETTINGS = new AiSettings(3);

    private Fakes() {
    }

    static final class Text implements TextGenerationPort {

        final List<String> prompts = new ArrayList<>();
        final List<String> systemInstructions = new ArrayList<>();
        final List<List<ChatMessage>> conversations = new ArrayList<>();
        Optional<String> answer = Optional.empty();

        @Override
        public Optional<String> generate(String prompt) {
            prompts.add(prompt);
            return answer;
        }

        @Override
        public Optional<String> converse(String systemInstruction, List<ChatMessage> messages) {
            systemInstructions.add(systemInstruction);
            conversations.add(messages);
            return answer;
        }

        int calls() {
            return prompts.size() + conversations.size();
        }
    }

    static final class RateLimit implements RateLimitPort {

        record Call(OrganizationId organizationId, int max, Duration window) {
        }

        final List<Call> calls = new ArrayList<>();
        boolean allow = true;

        @Override
        public boolean register(OrganizationId organizationId, int max, Duration window) {
            calls.add(new Call(organizationId, max, window));
            return allow;
        }
    }
}
