package app.lexo.application.service;

import app.lexo.application.port.in.GetUserQuery;
import app.lexo.application.port.in.ListUsersQuery;
import app.lexo.application.port.in.UserExistsQuery;
import app.lexo.application.port.in.UserResponse;
import app.lexo.application.port.out.ListUsersPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import app.lexo.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class UserQueryService implements ListUsersQuery, GetUserQuery, UserExistsQuery {

    private final ListUsersPort listUsersPort;
    private final LoadUserPort loadUserPort;
    private final UserExistsPort userExistsPort;

    UserQueryService(ListUsersPort listUsersPort, LoadUserPort loadUserPort, UserExistsPort userExistsPort) {
        this.listUsersPort = listUsersPort;
        this.loadUserPort = loadUserPort;
        this.userExistsPort = userExistsPort;
    }

    @Override
    public List<UserResponse> list(OrganizationId organizationId) {
        return listUsersPort.inOrganization(organizationId).stream().map(UserResponse::of).toList();
    }

    @Override
    public UserResponse get(OrganizationId organizationId, UserId userId) {
        return loadUserPort.inOrganization(organizationId, userId).map(UserResponse::of)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public boolean exists(OrganizationId organizationId, UserId userId) {
        return userExistsPort.existsInOrganization(organizationId, userId);
    }
}
