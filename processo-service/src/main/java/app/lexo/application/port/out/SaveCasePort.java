package app.lexo.application.port.out;

import app.lexo.domain.Case;

public interface SaveCasePort {

    Case save(Case value);
}
