package app.lexo.adapter.in.web;

import app.lexo.application.port.in.Actor;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.OrganizationId;

final class Identity {

    private Identity() {
    }

    static OrganizationId organization(AuthUser user) {
        return new OrganizationId(user.organizationId());
    }

    static Actor actor(AuthUser user) {
        return new Actor(user.id(), nameOf(user));
    }

    private static String nameOf(AuthUser user) {
        if (user.name() != null) {
            return user.name();
        }
        return user.email() != null ? user.email() : "Sistema";
    }
}
