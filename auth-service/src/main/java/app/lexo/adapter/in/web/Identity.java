package app.lexo.adapter.in.web;

import app.lexo.application.port.in.Actor;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;

final class Identity {

    private static final String DEFAULT_NAME = "Usuário";

    private Identity() {
    }

    static OrganizationId organization(AuthUser user) {
        return new OrganizationId(user.organizationId());
    }

    static UserId user(AuthUser user) {
        return new UserId(user.id());
    }

    static Actor actor(AuthUser user) {
        String name = user.name() != null ? user.name() : user.email() != null ? user.email() : DEFAULT_NAME;
        return new Actor(user(user), name);
    }
}
