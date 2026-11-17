package app.lexo.application.port.out;

public interface SendPasswordEmailPort {

    void sendResetLink(PasswordResetEmail email);

    void sendChangedNotice(PasswordChangedEmail email);
}
