package app.lexo.application.port.out;

import app.lexo.domain.User;

public interface SaveUserPort {

    User save(User user);
}
