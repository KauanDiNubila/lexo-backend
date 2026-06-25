package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * cliente-service: gestao de clientes (pessoa fisica/juridica) com seu proprio banco.
 * Registra no Eureka e publica eventos de dominio no Kafka.
 */
@SpringBootApplication
public class ClienteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClienteServiceApplication.class, args);
    }
}
