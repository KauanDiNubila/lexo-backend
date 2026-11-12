package app.lexo.application.port.out;

import app.lexo.domain.UserId;

public interface DeleteUserPort {

    void delete(UserId id);
}
