package app.lexo.service;

import app.lexo.domain.Deadline;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.email.EnfileiradorEmail;
import app.lexo.email.TarefaEmail;
import app.lexo.repository.DeadlineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Notificacao de prazos, portada de api/cron/notify-deadlines. Alerta prazos pendentes
 * (ainda nao notificados) que vencem nos proximos 3 dias e marca notifiedAt.
 */
@Service
public class NotificacaoPrazosService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPrazosService.class);

    private final DeadlineRepository repo;
    private final EnfileiradorEmail enfileiradorEmail;

    public NotificacaoPrazosService(DeadlineRepository repo, EnfileiradorEmail enfileiradorEmail) {
        this.repo = repo;
        this.enfileiradorEmail = enfileiradorEmail;
    }

    /** Roda todo dia as 08:00 (horario do servidor). */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public int executar() {
        Instant now = Instant.now();
        Instant limit = now.plus(3, ChronoUnit.DAYS);

        List<Deadline> due = repo.findByStatusAndNotifiedAtIsNullAndDateBetween(
                DeadlineStatus.PENDENTE, now, limit);

        for (Deadline d : due) {
            enfileiradorEmail.enfileirar(new TarefaEmail(
                    "LEMBRETE_PRAZO", "advogado-responsavel",
                    "Prazo próximo: " + d.getTitle(),
                    "O prazo \"" + d.getTitle() + "\" vence em " + d.getDate()));
            d.setNotifiedAt(now);
        }
        repo.saveAll(due);

        log.info("[cron] notificacao de prazos: {} lembrete(s) enviado(s)", due.size());
        return due.size();
    }
}
