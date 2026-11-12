package app.lexo.application.port.in;

public interface StartTwoFactorUseCase {

    TwoFactorSetup start(StartTwoFactorCommand command);
}
