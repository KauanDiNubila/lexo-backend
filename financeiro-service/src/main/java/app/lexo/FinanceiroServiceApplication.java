package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FinanceiroServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceiroServiceApplication.class, args);
    }
}
