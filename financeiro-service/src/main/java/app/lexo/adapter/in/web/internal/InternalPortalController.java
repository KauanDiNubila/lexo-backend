package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.ListClientFeesQuery;
import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class InternalPortalController {

    private final ListClientFeesQuery listClientFees;

    InternalPortalController(ListClientFeesQuery listClientFees) {
        this.listClientFees = listClientFees;
    }

    @GetMapping("/internal/portal/honorarios")
    List<PortalFee> byClient(@RequestParam String clientId, @RequestParam String orgId) {
        if (clientId.isBlank() || orgId.isBlank()) {
            return List.of();
        }
        return listClientFees.list(new OrganizationId(orgId), new ClientId(clientId)).stream()
                .map(fee -> new PortalFee(fee.description(), fee.amount(), fee.status().code(), fee.dueDate()))
                .toList();
    }
}
