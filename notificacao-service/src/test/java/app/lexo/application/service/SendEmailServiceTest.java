package app.lexo.application.service;

import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import app.lexo.application.port.out.MailPort;
import app.lexo.domain.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Send e-mail use case")
class SendEmailServiceTest {

    private static class RecordingMail implements MailPort {
        final List<Email> sent = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void send(Email email) {
            if (failure != null) {
                throw failure;
            }
            sent.add(email);
        }
    }

    private final RecordingMail mail = new RecordingMail();
    private final SendEmailService service = new SendEmailService(mail);

    @Test
    @DisplayName("hands the recipient, subject and body to the mail port")
    void sends() {
        service.send(new SendEmailCommand("CONVITE", "ana@cliente.com", "Convite", "Entre no Lexo"));

        assertThat(mail.sent).containsExactly(new Email("ana@cliente.com", "Convite", "Entre no Lexo"));
    }

    @Test
    @DisplayName("lets a delivery failure reach the caller so the message can be retried")
    void failureReachesTheCaller() {
        mail.failure = new IllegalStateException("smtp fora do ar");

        assertThatThrownBy(() -> service.send(new SendEmailCommand("CONVITE", "ana@cliente.com", "Convite", "Corpo")))
                .isSameAs(mail.failure);
    }
}
