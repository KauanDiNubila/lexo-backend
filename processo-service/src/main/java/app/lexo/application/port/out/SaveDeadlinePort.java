package app.lexo.application.port.out;

import app.lexo.domain.Deadline;

public interface SaveDeadlinePort {

    Deadline save(Deadline deadline);
}
