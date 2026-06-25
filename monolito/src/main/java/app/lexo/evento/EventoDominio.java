package app.lexo.evento;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de dominio publicado no Kafka quando algo relevante acontece.
 * Carrega o tenant (organizationId) e dados suficientes para um consumidor reagir
 * (ex.: gravar auditoria) sem precisar consultar o servico de origem.
 */
public record EventoDominio(
        String tipo,            // ex.: CLIENTE_CRIADO, PROCESSO_CRIADO, PROCESSO_ATUALIZADO
        String organizationId,
        String entityType,      // ex.: CLIENTE, PROCESSO
        String entityId,
        String descricao,
        String userId,
        String userName,
        Instant ocorridoEm
) implements Serializable {

    public static EventoDominio de(String tipo, String organizationId, String entityType,
                                   String entityId, String descricao, String userId, String userName) {
        return new EventoDominio(tipo, organizationId, entityType, entityId, descricao,
                userId, userName, Instant.now());
    }
}
