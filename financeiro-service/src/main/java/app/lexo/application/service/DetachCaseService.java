package app.lexo.application.service;

import app.lexo.application.port.in.DetachCaseCommand;
import app.lexo.application.port.in.DetachCaseUseCase;
import app.lexo.application.port.out.DetachCaseFromInvoicesPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DetachCaseService implements DetachCaseUseCase {

    private final DetachCaseFromInvoicesPort detachCaseFromInvoicesPort;

    DetachCaseService(DetachCaseFromInvoicesPort detachCaseFromInvoicesPort) {
        this.detachCaseFromInvoicesPort = detachCaseFromInvoicesPort;
    }

    @Override
    public int detach(DetachCaseCommand command) {
        return detachCaseFromInvoicesPort.detach(command.organizationId(), command.caseId());
    }
}
