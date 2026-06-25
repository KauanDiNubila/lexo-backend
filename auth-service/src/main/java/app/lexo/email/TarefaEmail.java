package app.lexo.email;

import java.io.Serializable;

/**
 * Tarefa de envio de e-mail trafegada pela fila do RabbitMQ. O servico de origem
 * monta o conteudo (assunto/corpo) e enfileira; um consumidor processa de forma assincrona.
 */
public record TarefaEmail(
        String tipo,          // ex.: CONVITE, LEMBRETE_PRAZO
        String destinatario,
        String assunto,
        String corpo
) implements Serializable {
}
