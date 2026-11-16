package app.lexo.application.port.in;

public interface SendEmailUseCase {

    void send(SendEmailCommand command);

    record SendEmailCommand(String type, String recipient, String subject, String body) {
    }
}
