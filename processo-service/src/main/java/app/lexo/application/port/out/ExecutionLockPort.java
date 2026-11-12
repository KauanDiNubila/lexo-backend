package app.lexo.application.port.out;

import java.time.Duration;

public interface ExecutionLockPort {

    boolean tryAcquire(String key, Duration ttl);
}
