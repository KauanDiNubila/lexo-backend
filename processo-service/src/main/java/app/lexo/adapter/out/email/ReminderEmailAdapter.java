package app.lexo.adapter.out.email;

import app.lexo.application.port.out.DeadlineReminder;
import app.lexo.application.port.out.SendReminderPort;
import app.lexo.common.email.EnfileiradorEmail;
import app.lexo.common.email.TarefaEmail;
import org.springframework.stereotype.Component;

@Component
class ReminderEmailAdapter implements SendReminderPort {

    private final EnfileiradorEmail emailQueue;

    ReminderEmailAdapter(EnfileiradorEmail emailQueue) {
        this.emailQueue = emailQueue;
    }

    @Override
    public void send(DeadlineReminder reminder) {
        emailQueue.enfileirar(new TarefaEmail(
                "LEMBRETE_PRAZO", reminder.recipientEmail(),
                "Prazo pr\u00f3ximo: " + reminder.deadlineTitle(),
                "Ol\u00e1 " + reminder.recipientName() + ", o prazo \"" + reminder.deadlineTitle()
                        + "\" vence em " + reminder.deadlineDate()));
    }
}
