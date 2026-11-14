package app.lexo.adapter.in.schedule;

import app.lexo.application.port.in.SendDeadlineRemindersUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class DeadlineReminderScheduler {

    private final SendDeadlineRemindersUseCase sendReminders;

    DeadlineReminderScheduler(SendDeadlineRemindersUseCase sendReminders) {
        this.sendReminders = sendReminders;
    }

    @Scheduled(cron = "0 0 8 * * *")
    void run() {
        sendReminders.sendReminders();
    }
}
