package app.lexo.application.service;

import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.domain.CaseData;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.ResponsibleNotFoundException;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

@Component
class CaseReferences {

    private final ClientExistsPort clientExistsPort;
    private final UserExistsPort userExistsPort;

    CaseReferences(ClientExistsPort clientExistsPort, UserExistsPort userExistsPort) {
        this.clientExistsPort = clientExistsPort;
        this.userExistsPort = userExistsPort;
    }

    void requireExist(OrganizationId organizationId, CaseData data) {
        if (!clientExistsPort.exists(organizationId, data.clientId())) {
            throw new ClientNotFoundException();
        }
        UserId responsible = data.responsibleId();
        if (responsible != null && !userExistsPort.exists(organizationId, responsible)) {
            throw new ResponsibleNotFoundException();
        }
    }
}
