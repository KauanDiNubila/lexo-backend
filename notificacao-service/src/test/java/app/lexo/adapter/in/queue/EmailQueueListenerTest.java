package app.lexo.adapter.in.queue;

import app.lexo.application.port.in.SendEmailUseCase.SendEmailCommand;
import app.lexo.common.email.TarefaEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("E-mail queue listener")
class EmailQueueListenerTest {

    @Test
    @DisplayName("turns a queued task into a send command")
    void translatesTheTask() {
        List<SendEmailCommand> received = new ArrayList<>();
        EmailQueueListener listener = new EmailQueueListener(received::add);

        listener.onTask(new TarefaEmail("CONVITE", "ana@cliente.com", "Convite", "Entre no Lexo"));

        assertThat(received).containsExactly(
                new SendEmailCommand("CONVITE", "ana@cliente.com", "Convite", "Entre no Lexo"));
    }

    @Test
    @DisplayName("lets a failure from the use case reach the broker so it retries and dead-letters")
    void failureReachesTheBroker() {
        IllegalStateException failure = new IllegalStateException("smtp fora do ar");
        EmailQueueListener listener = new EmailQueueListener(command -> {
            throw failure;
        });

        assertThatThrownBy(() -> listener.onTask(new TarefaEmail("CONVITE", "ana@cliente.com", "Convite", "Corpo")))
                .isSameAs(failure);
    }
}
