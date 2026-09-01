package app.lexo.client;

import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "processo-service")
public interface ProcessoPortalClient {

    @GetMapping("/internal/portal/processos")
    List<ProcessoPortal> processosDoCliente(@RequestParam("clientId") String clientId,
                                            @RequestParam("orgId") String orgId);
}
