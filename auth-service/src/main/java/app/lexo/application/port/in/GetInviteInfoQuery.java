package app.lexo.application.port.in;

public interface GetInviteInfoQuery {

    InviteInfo get(String token);
}
