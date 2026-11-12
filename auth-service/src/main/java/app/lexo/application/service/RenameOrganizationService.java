package app.lexo.application.service;

import app.lexo.application.port.in.OrganizationInfo;
import app.lexo.application.port.in.RenameOrganizationCommand;
import app.lexo.application.port.in.RenameOrganizationUseCase;
import app.lexo.application.port.out.CountUsersPort;
import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.application.port.out.SaveOrganizationPort;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RenameOrganizationService implements RenameOrganizationUseCase {

    private final LoadOrganizationPort loadOrganizationPort;
    private final SaveOrganizationPort saveOrganizationPort;
    private final CountUsersPort countUsersPort;

    RenameOrganizationService(LoadOrganizationPort loadOrganizationPort, SaveOrganizationPort saveOrganizationPort,
                              CountUsersPort countUsersPort) {
        this.loadOrganizationPort = loadOrganizationPort;
        this.saveOrganizationPort = saveOrganizationPort;
        this.countUsersPort = countUsersPort;
    }

    @Override
    public OrganizationInfo rename(RenameOrganizationCommand command) {
        Organization organization = loadOrganizationPort.byId(command.organizationId())
                .orElseThrow(OrganizationNotFoundException::new);
        organization.rename(command.name());
        Organization saved = saveOrganizationPort.save(organization);
        return OrganizationInfo.of(saved, countUsersPort.countInOrganization(command.organizationId()));
    }
}
