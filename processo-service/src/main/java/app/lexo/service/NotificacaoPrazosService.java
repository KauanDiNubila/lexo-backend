package app.lexo.service;

import app.lexo.client.ReferenciaGateway;
import app.lexo.common.email.TarefaEmail;
import app.lexo.common.usuario.UsuarioInterno;
import app.lexo.domain.Case;
import app.lexo.domain.Deadline;
import app.lexo.domain.enums.DeadlineStatus;
import app.lexo.common.email.EnfileiradorEmail;
import app.lexo.repository.CaseRepository;
import app.lexo.repository.DeadlineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificacaoPrazosService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPrazosService.class);

    private static final String CHAVE_LOCK = "lock:cron:notify-deadlines";
    private static final Duration TTL_LOCK = Duration.ofMinutes(5);

    private final DeadlineRepository repo;
    private final CaseRepository casos;
    private final ReferenciaGateway referencias;
    private final EnfileiradorEmail enfileiradorEmail;
    private final StringRedisTemplate redis;

    public NotificacaoPrazosService(DeadlineRepository repo, CaseRepository casos,
                                    ReferenciaGateway referencias, EnfileiradorEmail enfileiradorEmail,
                                    StringRedisTemplate redis) {
        this.repo = repo;
        this.casos = casos;
        this.referencias = referencias;
        this.enfileiradorEmail = enfileiradorEmail;
        this.redis = redis;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public int executar() {
        String token = UUID.randomUUID().toString();
        boolean adquiriu = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(CHAVE_LOCK, token, TTL_LOCK));

        if (!adquiriu) {
            log.info("[cron] notificacao de prazos ja esta rodando em outra instancia — pulando");
            return 0;
        }

        // O lock expira sozinho pelo TTL; nao ha release explicito de proposito —
        // bloquear uma segunda chamada dentro da janela de 5min e o comportamento
        // desejado (evita reenvio ao disparar o endpoint manual em seguida).

        Instant now = Instant.now();
        Instant limit = now.plus(3, ChronoUnit.DAYS);

        List<Deadline> due = repo.findByStatusAndNotifiedAtIsNullAndDateBetween(
                DeadlineStatus.PENDENTE, now, limit);

        if (due.isEmpty()) {
            log.info("[cron] notificacao de prazos: nenhum prazo pendente na janela");
            return 0;
        }

        Map<String, Case> casosPorId = casos.findAllById(
                due.stream().map(Deadline::getCaseId).distinct().toList())
                .stream().collect(Collectors.toMap(Case::getId, c -> c));

        List<Deadline> notificados = due.stream()
                .filter(d -> notificar(d, casosPorId.get(d.getCaseId())))
                .peek(d -> d.setNotifiedAt(now))
                .toList();

        repo.saveAll(notificados);

        log.info("[cron] notificacao de prazos: {} lembrete(s) enviado(s), {} pulado(s) sem responsavel",
                notificados.size(), due.size() - notificados.size());
        return notificados.size();
    }

    private boolean notificar(Deadline d, Case c) {
        if (c == null || c.getResponsavelId() == null || c.getResponsavelId().isBlank()) {
            log.warn("[cron] prazo \"{}\" (caso {}) sem responsavel atribuido — pulando",
                    d.getTitle(), d.getCaseId());
            return false;
        }

        UsuarioInterno responsavel = referencias.buscarUsuario(c.getResponsavelId(), d.getOrganizationId())
                .orElse(null);
        if (responsavel == null || responsavel.email() == null || responsavel.email().isBlank()) {
            log.warn("[cron] responsavel {} do prazo \"{}\" nao encontrado ou sem e-mail — pulando",
                    c.getResponsavelId(), d.getTitle());
            return false;
        }

        enfileiradorEmail.enfileirar(new TarefaEmail(
                "LEMBRETE_PRAZO", responsavel.email(),
                "Prazo próximo: " + d.getTitle(),
                "Olá " + responsavel.name() + ", o prazo \"" + d.getTitle() + "\" vence em " + d.getDate()));
        return true;
    }
}
