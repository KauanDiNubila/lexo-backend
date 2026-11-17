package app.lexo.application.port.out;

public interface SendPasswordEmailPort {

    boolean sendResetLink(PasswordResetEmail email);

    boolean sendChangedNotice(PasswordChangedEmail email);
}
