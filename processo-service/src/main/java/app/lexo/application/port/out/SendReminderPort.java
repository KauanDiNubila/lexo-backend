package app.lexo.application.port.out;

public interface SendReminderPort {

    boolean send(DeadlineReminder reminder);
}
