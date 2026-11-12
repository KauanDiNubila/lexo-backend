package app.lexo.application.service;

import app.lexo.application.port.in.DeleteClientCasesCommand;
import app.lexo.application.port.in.DeleteClientCasesUseCase;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.domain.Case;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
class DeleteClientCasesService implements DeleteClientCasesUseCase {

    private final LoadClientCasesPort loadClientCasesPort;
    private final CaseRemoval removal;

    DeleteClientCasesService(LoadClientCasesPort loadClientCasesPort, CaseRemoval removal) {
        this.loadClientCasesPort = loadClientCasesPort;
        this.removal = removal;
    }

    @Override
    public int deleteOfClient(DeleteClientCasesCommand command) {
        List<Case> cases = loadClientCasesPort.ofClient(command.organizationId(), command.clientId());
        cases.forEach(value -> removal.remove(command.organizationId(), value.id()));
        return cases.size();
    }
}
