package app.lexo.client;

import app.lexo.dto.PortalDtos.HonorarioPortal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Busca os honorarios de um cliente no financeiro-service, para o portal. */
@FeignClient(name = "financeiro-service")
public interface FinanceiroPortalClient {

    @GetMapping("/internal/portal/honorarios")
    List<HonorarioPortal> honorariosDoCliente(@RequestParam("clientId") String clientId,
                                              @RequestParam("orgId") String orgId);
}
