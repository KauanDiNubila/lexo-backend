package app.lexo.client;

import app.lexo.controller.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReferenciaGateway {

    private static final Logger log = LoggerFactory.getLogger(ReferenciaGateway.class);

    private final ClienteServiceClient clienteClient;
    private final ProcessoServiceClient processoClient;

    public ReferenciaGateway(ClienteServiceClient clienteClient, ProcessoServiceClient processoClient) {
        this.clienteClient = clienteClient;
        this.processoClient = processoClient;
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

    @CircuitBreaker(name = "processo-service", fallbackMethod = "processoExisteFallback")
    public boolean processoExiste(String id, String orgId) {
        return Boolean.TRUE.equals(processoClient.processoExiste(id, orgId).get("existe"));
    }

    @SuppressWarnings("unused")
    private boolean processoExisteFallback(String id, String orgId, Throwable causa) {
        log.warn("processo-service indisponivel (circuit breaker): {}", causa.toString());
        throw ApiException.serviceUnavailable(
                "Serviço de processos temporariamente indisponível. Tente novamente em instantes.");
    }
}
