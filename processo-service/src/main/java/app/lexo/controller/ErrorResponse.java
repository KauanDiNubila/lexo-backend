package app.lexo.controller;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String path
) {
    public static ErrorResponse of(int status, String error, String path) {
        return new ErrorResponse(Instant.now(), status, error, path);
    }
}
