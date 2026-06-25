package app.lexo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: ponto de entrada unico da plataforma. Descobre os servicos pelo Eureka
 * e roteia as requisicoes para eles (lb://nome-do-servico).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
