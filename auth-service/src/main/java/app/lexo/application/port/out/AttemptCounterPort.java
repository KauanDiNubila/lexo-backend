package app.lexo.application.port.out;

import java.time.Duration;

public interface AttemptCounterPort {

    boolean register(String key, int max, Duration window);

    void reset(String key);
}
