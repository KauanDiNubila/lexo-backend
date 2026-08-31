package app.lexo.controller;

import app.lexo.service.NotificacaoPrazosService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/cron")
public class CronController {

    private final NotificacaoPrazosService notifier;

    public CronController(NotificacaoPrazosService notifier) {
        this.notifier = notifier;
    }

    @PostMapping("/notify-deadlines")
    public Map<String, Object> notifyDeadlines() {
        int sent = notifier.executar();
        return Map.of("ok", true, "sent", sent);
    }
}
