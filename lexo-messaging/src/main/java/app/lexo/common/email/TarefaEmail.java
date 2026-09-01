package app.lexo.common.email;

import java.io.Serializable;

public record TarefaEmail(
        String tipo,
        String destinatario,
        String assunto,
        String corpo
) implements Serializable {
}
