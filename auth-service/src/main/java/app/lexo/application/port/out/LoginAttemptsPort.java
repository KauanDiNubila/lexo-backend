package app.lexo.application.port.out;

import java.time.Duration;

public interface LoginAttemptsPort {

    boolean register(String key, int max, Duration window);

    void reset(String key);
}
