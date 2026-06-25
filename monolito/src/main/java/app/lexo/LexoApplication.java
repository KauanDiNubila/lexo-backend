package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LexoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LexoApplication.class, args);
    }
}
