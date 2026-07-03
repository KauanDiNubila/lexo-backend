package app.lexo.email;

import java.io.Serializable;

public record TarefaEmail(
        String tipo,
        String destinatario,
        String assunto,
        String corpo
) implements Serializable {
}
