package app.lexo.adapter.out.email;

import app.lexo.application.port.out.DeadlineReminder;
import app.lexo.application.port.out.SendReminderPort;
import app.lexo.common.email.EmailPublisher;
import app.lexo.common.email.EmailTask;
import org.springframework.stereotype.Component;

@Component
class ReminderEmailAdapter implements SendReminderPort {

    private final EmailPublisher emailQueue;

    ReminderEmailAdapter(EmailPublisher emailQueue) {
        this.emailQueue = emailQueue;
    }

    @Override
    public boolean send(DeadlineReminder reminder) {
        return emailQueue.enqueue(new EmailTask(
                "LEMBRETE_PRAZO", reminder.recipientEmail(),
                "Prazo pr\u00f3ximo: " + reminder.deadlineTitle(),
                "Ol\u00e1 " + reminder.recipientName() + ", o prazo \"" + reminder.deadlineTitle()
                        + "\" vence em " + reminder.deadlineDate()));
    }
}
