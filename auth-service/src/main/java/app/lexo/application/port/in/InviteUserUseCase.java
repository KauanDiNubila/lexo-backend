package app.lexo.application.port.in;

public interface InviteUserUseCase {

    InviteResponse invite(InviteUserCommand command);
}
