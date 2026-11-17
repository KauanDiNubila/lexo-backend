package app.lexo.application.port.in;

import app.lexo.domain.EmailAddress;

public record RequestPasswordResetCommand(EmailAddress email) {
}
