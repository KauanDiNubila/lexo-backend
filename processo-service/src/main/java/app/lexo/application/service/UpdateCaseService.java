package app.lexo.application.service;

import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.UpdateCaseCommand;
import app.lexo.application.port.in.UpdateCaseUseCase;
import app.lexo.application.port.out.CaseEvent;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveCasePort;
import app.lexo.domain.Case;
import app.lexo.domain.CaseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class UpdateCaseService implements UpdateCaseUseCase {

    private final CaseReferences references;
    private final LoadCasePort loadCasePort;
    private final SaveCasePort saveCasePort;
    private final PublishEventPort publishEventPort;

    UpdateCaseService(CaseReferences references, LoadCasePort loadCasePort, SaveCasePort saveCasePort,
                      PublishEventPort publishEventPort) {
        this.references = references;
        this.loadCasePort = loadCasePort;
        this.saveCasePort = saveCasePort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public CaseResponse update(UpdateCaseCommand command) {
        references.requireExist(command.organizationId(), command.data());
        Case value = loadCasePort.byId(command.organizationId(), command.caseId())
                .orElseThrow(CaseNotFoundException::new);
        value.update(command.data());
        Case saved = saveCasePort.save(value);
        publishEventPort.publish(CaseEvent.updated(saved, command.actor()));
        return CaseResponse.of(saved);
    }
}
