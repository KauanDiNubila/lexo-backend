package app.lexo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Cliente Feign para o auth-service. O nome "auth-service" e resolvido via Eureka
 * (load-balanced). Usado para validar dados de usuario que agora vivem em outro servico.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/internal/usuarios/{id}/existe")
    Map<String, Boolean> usuarioExiste(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
