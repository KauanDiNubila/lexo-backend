package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * cliente-service: gestao de clientes (pessoa fisica/juridica) com seu proprio banco.
 * Registra no Eureka e publica eventos de dominio no Kafka.
 */
@SpringBootApplication
@EnableFeignClients
public class ClienteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClienteServiceApplication.class, args);
    }
}
