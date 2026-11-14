package app.lexo.domain;

import java.time.Instant;

public record MovementData(String title, String description, Instant date) {
}
