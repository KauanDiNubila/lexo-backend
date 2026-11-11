package app.lexo.adapter.out.finance;

import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "financeiro-service")
public interface FinancePortalClient {

    @GetMapping("/internal/portal/honorarios")
    List<HonorarioPortal> feesOfClient(@RequestParam("clientId") String clientId,
                                              @RequestParam("orgId") String orgId);
}
