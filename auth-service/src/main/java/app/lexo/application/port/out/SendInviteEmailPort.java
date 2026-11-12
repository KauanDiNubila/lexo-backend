package app.lexo.application.port.out;

public interface SendInviteEmailPort {

    boolean send(InviteEmail email);
}
