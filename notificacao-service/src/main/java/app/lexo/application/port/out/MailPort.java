package app.lexo.application.port.out;

import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;

public interface MailPort {

    void send(SendEmailCommand command);
}
