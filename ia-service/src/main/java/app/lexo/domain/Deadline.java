package app.lexo.domain;

import java.util.Locale;
import java.util.Set;

public record Deadline(String title, String date, String status) {

    private static final Set<String> CLOSED = Set.of("CONCLUIDO", "PERDIDO");

    public boolean isOpen() {
        return status == null || !CLOSED.contains(status.trim().toUpperCase(Locale.ROOT));
    }
}
