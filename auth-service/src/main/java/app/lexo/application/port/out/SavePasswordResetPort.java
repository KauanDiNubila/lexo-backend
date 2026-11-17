package app.lexo.application.port.out;

import app.lexo.domain.PasswordReset;

public interface SavePasswordResetPort {

    PasswordReset save(PasswordReset reset);
}
