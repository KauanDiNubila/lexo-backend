package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * auditoria-service: consome eventos de dominio do Kafka e mantem o log de auditoria
 * (com seu proprio banco). Expoe a leitura do log para administradores.
 */
@SpringBootApplication
public class AuditoriaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditoriaServiceApplication.class, args);
    }
}
