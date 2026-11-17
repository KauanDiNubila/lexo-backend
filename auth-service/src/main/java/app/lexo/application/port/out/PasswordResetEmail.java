package app.lexo.application.port.out;

import app.lexo.domain.EmailAddress;

public record PasswordResetEmail(EmailAddress to, String name, String token) {
}
