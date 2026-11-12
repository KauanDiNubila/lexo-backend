package app.lexo.application.port.in;

import app.lexo.domain.UserId;

public record Actor(UserId userId, String name) {
}
