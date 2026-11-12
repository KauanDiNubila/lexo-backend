package app.lexo.application.port.out;

import app.lexo.domain.RefreshToken;

import java.util.Optional;

public interface LoadRefreshTokenPort {

    Optional<RefreshToken> byHash(String tokenHash);
}
