package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * processo-service: processos judiciais, agenda (prazos) e atividades, com banco proprio.
 * Valida cliente/responsavel via Feign, publica eventos no Kafka e enfileira lembretes
 * de prazo no RabbitMQ (cron diario).
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class ProcessoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessoServiceApplication.class, args);
    }
}
