package app.lexo.application.port.out;

import app.lexo.domain.Deadline;

import java.time.Instant;
import java.util.List;

public interface LoadDeadlinesToRemindPort {

    List<Deadline> pendingNotNotifiedBetween(Instant from, Instant to);
}
