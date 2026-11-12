package app.lexo.application.port.out;

import app.lexo.domain.UserInvite;

public interface SaveInvitePort {

    UserInvite save(UserInvite invite);
}
