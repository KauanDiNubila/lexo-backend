package app.lexo.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class RefreshCookie {

    static final String NAME = "lexo_refresh_token";

    private static final String PATH = "/api/auth";

    private final long validityDays;

    RefreshCookie(@Value("${lexo.refresh-expiration-days}") long validityDays) {
        this.validityDays = validityDays;
    }

    void attach(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(token, Duration.ofDays(validityDays)).toString());
    }

    void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
