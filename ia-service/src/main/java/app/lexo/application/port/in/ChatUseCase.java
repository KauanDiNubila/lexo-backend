package app.lexo.application.port.in;

public interface ChatUseCase {

    ChatResponse chat(ChatCommand command);
}
