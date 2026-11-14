package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;

public record InviteEmail(EmailAddress to, String inviteeName, String organizationName, String token) {
}
