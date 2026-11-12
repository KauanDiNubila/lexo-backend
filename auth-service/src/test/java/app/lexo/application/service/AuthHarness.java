package app.lexo.application.service;

import app.lexo.application.port.in.Actor;
import app.lexo.common.security.Role;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserId;

class AuthHarness {

    static final String PASSWORD = "password-1";

    final InMemoryAuth store = new InMemoryAuth();
    final Fakes.Hasher hasher = new Fakes.Hasher();
    final Fakes.AccessTokens accessTokens = new Fakes.AccessTokens();
    final Fakes.Cipher cipher = new Fakes.Cipher();
    final Fakes.Totp totp = new Fakes.Totp();
    final Fakes.Attempts attempts = new Fakes.Attempts();
    final Fakes.Events events = new Fakes.Events();
    final Fakes.InviteEmails emails = new Fakes.InviteEmails();
    final SessionIssuer sessions = new SessionIssuer(store, accessTokens, Fakes.CLOCK, 30);

    Organization addOrganization(String name) {
        return store.save(Organization.register(name, Fakes.NOW));
    }

    User addUser(OrganizationId organizationId, String name, String email, Role role) {
        return store.save(User.register(organizationId, name, new EmailAddress(email), hasher.hash(PASSWORD), role));
    }

    static Actor actor(User user) {
        return new Actor(user.id(), user.name());
    }

    static UserId unknownUser() {
        return new UserId("nobody");
    }
}
