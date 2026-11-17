package app.lexo.application.port.out;

import app.lexo.domain.UserId;

public interface DeletePasswordResetsPort {

    void ofUser(UserId userId);
}
