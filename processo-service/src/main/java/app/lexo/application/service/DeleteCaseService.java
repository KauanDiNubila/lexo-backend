package app.lexo.application.service;

import app.lexo.application.port.in.DeleteCaseCommand;
import app.lexo.application.port.in.DeleteCaseUseCase;
import app.lexo.application.port.out.CaseEvent;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.domain.Case;
import app.lexo.domain.CaseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DeleteCaseService implements DeleteCaseUseCase {

    private final LoadCasePort loadCasePort;
    private final CaseRemoval removal;
    private final PublishEventPort publishEventPort;

    DeleteCaseService(LoadCasePort loadCasePort, CaseRemoval removal, PublishEventPort publishEventPort) {
        this.loadCasePort = loadCasePort;
        this.removal = removal;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public void delete(DeleteCaseCommand command) {
        Case value = loadCasePort.byId(command.organizationId(), command.caseId())
                .orElseThrow(CaseNotFoundException::new);
        removal.remove(command.organizationId(), command.caseId());
        publishEventPort.publish(CaseEvent.deleted(value, command.actor()));
    }
}
