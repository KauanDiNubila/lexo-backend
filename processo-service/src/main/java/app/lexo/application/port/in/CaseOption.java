package app.lexo.application.port.in;

import java.io.Serializable;

public record CaseOption(String id, String number, String clientId) implements Serializable {
}
