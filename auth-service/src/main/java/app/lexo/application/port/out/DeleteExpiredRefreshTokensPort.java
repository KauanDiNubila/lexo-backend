package app.lexo.application.port.out;

import java.time.Instant;

public interface DeleteExpiredRefreshTokensPort {

    int deleteExpiredBefore(Instant cutoff);
}
