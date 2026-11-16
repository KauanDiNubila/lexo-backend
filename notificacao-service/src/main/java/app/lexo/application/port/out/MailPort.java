package app.lexo.application.port.out;

import app.lexo.domain.Email;

public interface MailPort {

    void send(Email email);
}
