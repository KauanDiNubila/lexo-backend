package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * notificacao-service: consome a fila de e-mails do RabbitMQ e dispara os envios.
 * Servico sem banco — apenas reage a tarefas enfileiradas por outros servicos.
 */
@SpringBootApplication
public class NotificacaoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacaoServiceApplication.class, args);
    }
}
