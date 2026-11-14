package app.lexo.adapter.in.web;

import app.lexo.common.security.AuthUser;
import app.lexo.domain.OrganizationId;

final class Identity {

    private Identity() {
    }

    static OrganizationId organization(AuthUser user) {
        return new OrganizationId(user.organizationId());
    }
}
