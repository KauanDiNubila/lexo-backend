package app.lexo.application.service;

import app.lexo.application.port.in.AccountResponse;
import app.lexo.application.port.in.GetAccountQuery;
import app.lexo.application.port.in.OrganizationInfo;
import app.lexo.application.port.in.ProfileInfo;
import app.lexo.application.port.out.CountUsersPort;
import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.OrganizationNotFoundException;
import app.lexo.domain.User;
import app.lexo.domain.UserId;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AccountQueryService implements GetAccountQuery {

    private final LoadOrganizationPort loadOrganizationPort;
    private final LoadUserPort loadUserPort;
    private final CountUsersPort countUsersPort;

    AccountQueryService(LoadOrganizationPort loadOrganizationPort, LoadUserPort loadUserPort,
                        CountUsersPort countUsersPort) {
        this.loadOrganizationPort = loadOrganizationPort;
        this.loadUserPort = loadUserPort;
        this.countUsersPort = countUsersPort;
    }

    @Override
    public AccountResponse get(OrganizationId organizationId, UserId userId) {
        Organization organization = loadOrganizationPort.byId(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);
        User user = loadUserPort.inOrganization(organizationId, userId).orElseThrow(UserNotFoundException::new);
        return new AccountResponse(OrganizationInfo.of(organization, countUsersPort.countInOrganization(organizationId)),
                ProfileInfo.of(user));
    }
}
