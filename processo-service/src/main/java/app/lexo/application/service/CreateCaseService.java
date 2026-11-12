package app.lexo.application.service;

import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.CreateCaseCommand;
import app.lexo.application.port.in.CreateCaseUseCase;
import app.lexo.application.port.out.CaseEvent;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.SaveCasePort;
import app.lexo.domain.Case;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CreateCaseService implements CreateCaseUseCase {

    private final CaseReferences references;
    private final SaveCasePort saveCasePort;
    private final PublishEventPort publishEventPort;

    CreateCaseService(CaseReferences references, SaveCasePort saveCasePort, PublishEventPort publishEventPort) {
        this.references = references;
        this.saveCasePort = saveCasePort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public CaseResponse create(CreateCaseCommand command) {
        references.requireExist(command.organizationId(), command.data());
        Case saved = saveCasePort.save(Case.open(command.organizationId(), command.data()));
        publishEventPort.publish(CaseEvent.created(saved, command.actor()));
        return CaseResponse.of(saved);
    }
}
