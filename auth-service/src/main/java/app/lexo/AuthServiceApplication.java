package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * auth-service: autenticacao, usuarios, organizacoes, 2FA e gestao de equipe.
 * Tem seu proprio banco (database-per-service) e se registra no Eureka.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
