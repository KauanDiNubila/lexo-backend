package app.lexo.common.web;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String error,
        String path
) {
    public static ErrorResponse of(int status, String code, String error, String path) {
        return new ErrorResponse(Instant.now(), status, code, error, path);
    }
}
