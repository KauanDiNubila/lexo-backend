package app.lexo.evento;

import java.io.Serializable;
import java.time.Instant;

public record EventoDominio(
        String tipo,
        String organizationId,
        String entityType,
        String entityId,
        String descricao,
        String userId,
        String userName,
        Instant ocorridoEm
) implements Serializable {
}
