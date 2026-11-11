package app.lexo.adapter.in.web;

import app.lexo.application.port.in.Actor;
import app.lexo.application.port.in.SortOrder;
import app.lexo.application.port.in.PageParams;
import app.lexo.common.security.AuthUser;
import app.lexo.domain.OrganizationId;
import org.springframework.data.domain.Pageable;

final class Identity {

    private Identity() {
    }

    static OrganizationId organization(AuthUser user) {
        return new OrganizationId(user.organizationId());
    }

    static Actor actor(AuthUser user) {
        return new Actor(user.id(), nameOf(user));
    }

    static PageParams pageParams(Pageable pageable) {
        return new PageParams(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().stream().map(o -> new SortOrder(o.getProperty(), o.isAscending())).toList());
    }

    private static String nameOf(AuthUser user) {
        if (user.name() != null) {
            return user.name();
        }
        return user.email() != null ? user.email() : "Sistema";
    }
}
