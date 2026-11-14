package app.lexo.adapter.in.web.internal;

import app.lexo.application.port.in.GetUserQuery;
import app.lexo.application.port.in.UserExistsQuery;
import app.lexo.application.port.in.UserResponse;
import app.lexo.common.reference.ExistsResponse;
import app.lexo.common.user.InternalUser;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalUserController {

    private final UserExistsQuery userExistsQuery;
    private final GetUserQuery getUserQuery;

    InternalUserController(UserExistsQuery userExistsQuery, GetUserQuery getUserQuery) {
        this.userExistsQuery = userExistsQuery;
        this.getUserQuery = getUserQuery;
    }

    @GetMapping("/internal/usuarios/{id}/existe")
    ExistsResponse exists(@PathVariable String id, @RequestParam String orgId) {
        return new ExistsResponse(userExistsQuery.exists(new OrganizationId(orgId), new UserId(id)));
    }

    @GetMapping("/internal/usuarios/{id}")
    InternalUser find(@PathVariable String id, @RequestParam String orgId) {
        UserResponse user = getUserQuery.get(new OrganizationId(orgId), new UserId(id));
        return new InternalUser(user.id(), user.name(), user.email());
    }
}
