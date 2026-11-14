package app.lexo.application.port.out;

import java.time.Duration;

public interface OncePerWindowPort {

    boolean tryClaim(String key, Duration window);
}
