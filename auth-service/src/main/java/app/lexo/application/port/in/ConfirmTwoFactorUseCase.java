package app.lexo.application.port.in;

public interface ConfirmTwoFactorUseCase {

    void confirm(ConfirmTwoFactorCommand command);
}
