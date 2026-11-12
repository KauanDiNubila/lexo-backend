package app.lexo.application.port.out;

import app.lexo.domain.User;

public interface IssueAccessTokenPort {

    String issue(User user);
}
