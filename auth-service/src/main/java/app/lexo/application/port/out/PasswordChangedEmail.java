package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;

public record PasswordChangedEmail(EmailAddress to, String name) {
}
