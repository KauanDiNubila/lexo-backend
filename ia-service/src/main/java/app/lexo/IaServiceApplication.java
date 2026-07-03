package app.lexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ia-service: recursos de IA do Lexo. Resume processos usando o Google Gemini (free tier);
 * sem chave configurada, cai num resumo heuristico (mock) — funciona com custo zero.
 * Fica atras do gateway (so requisicoes autenticadas chegam aqui).
 */
@SpringBootApplication
public class IaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IaServiceApplication.class, args);
    }
}
