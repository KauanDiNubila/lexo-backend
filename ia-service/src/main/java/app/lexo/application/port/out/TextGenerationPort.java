package app.lexo.application.port.out;

import app.lexo.domain.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface TextGenerationPort {

    Optional<String> generate(String prompt);

    Optional<String> converse(String systemInstruction, List<ChatMessage> messages);
}
