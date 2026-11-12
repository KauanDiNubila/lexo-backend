package app.lexo.application.port.out;

import app.lexo.domain.RefreshToken;

public interface SaveRefreshTokenPort {

    RefreshToken save(RefreshToken token);
}
