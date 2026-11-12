package app.lexo.application.port.in;

public record SessionResponse(String accessToken, UserInfo user, String refreshToken) {
}
