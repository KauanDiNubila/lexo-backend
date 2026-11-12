package app.lexo.application.port.out;

public interface SendReminderPort {

    void send(DeadlineReminder reminder);
}
