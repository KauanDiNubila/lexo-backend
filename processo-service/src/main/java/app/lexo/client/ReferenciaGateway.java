package app.lexo.client;

import app.lexo.controller.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fachada resiliente para as chamadas entre servicos. Cada metodo e protegido por um
 * circuit breaker (Resilience4j): se o servico destino estiver fora do ar ou lento, o
 * circuito abre e o fallback devolve um erro controlado (503) em vez de travar a requisicao
 * ou vazar um 500 generico. Isola a falha de um servico para que ela nao derrube este.
 */
@Component
public class ReferenciaGateway {

    private static final Logger log = LoggerFactory.getLogger(ReferenciaGateway.class);

    private final ClienteServiceClient clienteClient;
    private final AuthServiceClient authClient;

    public ReferenciaGateway(ClienteServiceClient clienteClient, AuthServiceClient authClient) {
        this.clienteClient = clienteClient;
        this.authClient = authClient;
    }

    @CircuitBreaker(name = "cliente-service", fallbackMethod = "clienteExisteFallback")
    public boolean clienteExiste(String id, String orgId) {
        return Boolean.TRUE.equals(clienteClient.clienteExiste(id, orgId).get("existe"));
    }

    @SuppressWarnings("unused")
    private boolean clienteExisteFallback(String id, String orgId, Throwable causa) {
        log.warn("cliente-service indisponivel (circuit breaker): {}", causa.toString());
        throw ApiException.serviceUnavailable(
                "Serviço de clientes temporariamente indisponível. Tente novamente em instantes.");
    }

    @CircuitBreaker(name = "auth-service", fallbackMethod = "usuarioExisteFallback")
    public boolean usuarioExiste(String id, String orgId) {
        return Boolean.TRUE.equals(authClient.usuarioExiste(id, orgId).get("existe"));
    }

    @SuppressWarnings("unused")
    private boolean usuarioExisteFallback(String id, String orgId, Throwable causa) {
        log.warn("auth-service indisponivel (circuit breaker): {}", causa.toString());
        throw ApiException.serviceUnavailable(
                "Serviço de usuários temporariamente indisponível. Tente novamente em instantes.");
    }
}
