package app.lexo.adapter.out.mail;

import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("SMTP mail adapter")
class SmtpMailAdapterTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpMailAdapter adapter = new SmtpMailAdapter(mailSender, "nao-responda@lexo.app");

    @Test
    @DisplayName("sends a plain message from the configured sender")
    void sendsPlainMessage() {
        adapter.send(new SendEmailCommand("CONVITE", "ana@cliente.com", "Convite", "Entre no Lexo"));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getFrom()).isEqualTo("nao-responda@lexo.app");
        assertThat(sent.getValue().getTo()).containsExactly("ana@cliente.com");
        assertThat(sent.getValue().getSubject()).isEqualTo("Convite");
        assertThat(sent.getValue().getText()).isEqualTo("Entre no Lexo");
    }

    @Test
    @DisplayName("does not hide a delivery failure")
    void failureIsNotHidden() {
        MailSendException failure = new MailSendException("smtp fora do ar");
        doThrow(failure).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.send(new SendEmailCommand("CONVITE", "ana@cliente.com", "Convite", "Corpo"))).isSameAs(failure);
    }
}
