package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.SendDeadlineRemindersUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/cron")
class InternalCronController {

    private final SendDeadlineRemindersUseCase sendReminders;

    InternalCronController(SendDeadlineRemindersUseCase sendReminders) {
        this.sendReminders = sendReminders;
    }

    @PostMapping("/notify-deadlines")
    Map<String, Object> notifyDeadlines() {
        return Map.of("ok", true, "sent", sendReminders.sendReminders());
    }
}
