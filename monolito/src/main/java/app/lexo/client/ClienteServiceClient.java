package app.lexo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Cliente Feign para o cliente-service. Resolve "cliente-service" via Eureka
 * (load-balanced). Usado para validar a existencia de um cliente que agora vive em outro servico.
 */
@FeignClient(name = "cliente-service")
public interface ClienteServiceClient {

    @GetMapping("/internal/clientes/{id}/existe")
    Map<String, Boolean> clienteExiste(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
