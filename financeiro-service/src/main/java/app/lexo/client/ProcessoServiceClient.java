package app.lexo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Cliente Feign para o processo-service. Resolve "processo-service" via Eureka.
 * Usado para validar a existencia de um processo (ex.: ao vincular um honorario).
 */
@FeignClient(name = "processo-service")
public interface ProcessoServiceClient {

    @GetMapping("/internal/processos/{id}/existe")
    Map<String, Boolean> processoExiste(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
