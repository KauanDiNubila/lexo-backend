package app.lexo.controller;

import app.lexo.service.NotificacaoPrazosService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cron")
public class CronController {

    private final NotificacaoPrazosService notifier;
    private final String cronSecret;

    public CronController(NotificacaoPrazosService notifier,
                          @Value("${CRON_SECRET:}") String cronSecret) {
        this.notifier = notifier;
        this.cronSecret = cronSecret;
    }

    @PostMapping("/notify-deadlines")
    public Map<String, Object> notifyDeadlines(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (cronSecret == null || cronSecret.isBlank()
                || !("Bearer " + cronSecret).equals(authorization)) {
            throw ApiException.unauthorized("Não autorizado");
        }
        int sent = notifier.executar();
        return Map.of("ok", true, "sent", sent);
    }
}
