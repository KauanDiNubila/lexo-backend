package app.lexo.application.service;

import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.InvoiceData;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

@Component
class InvoiceReferences {

    private final ClientExistsPort clientExistsPort;
    private final CaseExistsPort caseExistsPort;

    InvoiceReferences(ClientExistsPort clientExistsPort, CaseExistsPort caseExistsPort) {
        this.clientExistsPort = clientExistsPort;
        this.caseExistsPort = caseExistsPort;
    }

    void requireExist(OrganizationId organizationId, InvoiceData data) {
        if (!clientExistsPort.exists(organizationId, data.clientId())) {
            throw new ClientNotFoundException();
        }
        if (data.caseId() != null && !caseExistsPort.exists(organizationId, data.caseId())) {
            throw new CaseNotFoundException();
        }
    }
}
