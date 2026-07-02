package app.lexo.client;

import app.lexo.dto.PortalDtos.ProcessoPortal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Busca os processos (e prazos) de um cliente no processo-service, para o portal. */
@FeignClient(name = "processo-service")
public interface ProcessoPortalClient {

    @GetMapping("/internal/portal/processos")
    List<ProcessoPortal> processosDoCliente(@RequestParam("clientId") String clientId,
                                            @RequestParam("orgId") String orgId);
}
