package app.lexo.evento;

import app.lexo.service.FinanceiroService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorEventos {

    private static final String TOPICO = "lexo.eventos";

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final FinanceiroService financeiro;

    public ConsumidorEventos(FinanceiroService financeiro) {
        this.financeiro = financeiro;
    }

    @KafkaListener(topics = TOPICO, groupId = "lexo-financeiro")
    public void consumir(EventoDominio evento) {
        switch (evento.tipo()) {
            case "CLIENTE_EXCLUIDO" -> {
                int apagados = financeiro.excluirPorCliente(evento.organizationId(), evento.entityId());
                log.info("[kafka] cliente {} excluido — {} honorario(s) removido(s) em cascata",
                        evento.entityId(), apagados);
            }
            case "PROCESSO_EXCLUIDO" -> {
                int desvinculados = financeiro.desvincularProcesso(
                        evento.organizationId(), evento.entityId());
                log.info("[kafka] processo {} excluido — {} honorario(s) desvinculado(s)",
                        evento.entityId(), desvinculados);
            }
            default -> { }
        }
    }
}
