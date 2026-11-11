package app.lexo.adapter.out.cases;

import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.client.ReferenciaGateway;
import app.lexo.domain.CaseId;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

@Component
class CaseExistsAdapter implements CaseExistsPort {

    private final ReferenciaGateway gateway;

    CaseExistsAdapter(ReferenciaGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean exists(OrganizationId organizationId, CaseId caseId) {
        return gateway.processoExiste(caseId.value(), organizationId.value());
    }
}
