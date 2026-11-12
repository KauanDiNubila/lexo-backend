package app.lexo.application.port.out;

import app.lexo.application.port.in.Actor;
import app.lexo.common.event.EventType;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserInvite;

public record AccountEvent(EventType type, OrganizationId organizationId, String entityType, String entityId,
                           String description, Actor actor) {

    private static final String USER_ENTITY = "USUARIO";

    public static AccountEvent userInvited(OrganizationId organizationId, Actor actor, String name,
                                           EmailAddress email, Role role) {
        return new AccountEvent(EventType.USER_INVITED, organizationId, USER_ENTITY, null,
                "Convidou " + name + " (" + email.value() + ") como " + role, actor);
    }

    public static AccountEvent inviteRevoked(OrganizationId organizationId, Actor actor, UserInvite invite) {
        return new AccountEvent(EventType.INVITE_REVOKED, organizationId, USER_ENTITY, null,
                "Revogou convite de " + invite.email().value(), actor);
    }

    public static AccountEvent roleChanged(OrganizationId organizationId, Actor actor, User user) {
        return new AccountEvent(EventType.ROLE_CHANGED, organizationId, USER_ENTITY, user.id().value(),
                "Alterou papel do usuário para " + user.role(), actor);
    }

    public static AccountEvent userRemoved(OrganizationId organizationId, Actor actor, User user) {
        return new AccountEvent(EventType.USER_REMOVED, organizationId, USER_ENTITY, user.id().value(),
                "Removeu " + user.name() + " (" + user.email().value() + ")", actor);
    }

    public static AccountEvent twoFactorEnabled(OrganizationId organizationId, Actor actor) {
        return new AccountEvent(EventType.TWO_FACTOR_ENABLED, organizationId, null, null,
                "Ativou verificação em dois fatores", actor);
    }

    public static AccountEvent twoFactorDisabled(OrganizationId organizationId, Actor actor) {
        return new AccountEvent(EventType.TWO_FACTOR_DISABLED, organizationId, null, null,
                "Desativou verificação em dois fatores", actor);
    }
}
