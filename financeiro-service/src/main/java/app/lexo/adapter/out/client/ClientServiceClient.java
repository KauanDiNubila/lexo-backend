package app.lexo.adapter.out.client;

import app.lexo.common.referencia.ExisteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cliente-service")
public interface ClientServiceClient {

    @GetMapping("/internal/clientes/{id}/existe")
    ExisteResponse clientExists(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
