package app.lexo.application.port.out;

import java.time.Instant;

public record DeadlineReminder(String recipientName, String recipientEmail, String deadlineTitle, Instant deadlineDate) {
}
