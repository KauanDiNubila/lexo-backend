package app.lexo.adapter.out.cases;

import app.lexo.common.reference.ExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "processo-service")
public interface CaseServiceClient {

    @GetMapping("/internal/processos/{id}/existe")
    ExistsResponse caseExists(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
