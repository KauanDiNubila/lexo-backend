package app.lexo.application.port.in;

public record InviteInfo(String organizationName, String inviteeName, String email, boolean valid,
                         String reason) {
}
