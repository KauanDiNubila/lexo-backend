package app.lexo.application.port.out;

import app.lexo.domain.CaseMovement;

public interface SaveMovementPort {

    CaseMovement save(CaseMovement movement);
}
