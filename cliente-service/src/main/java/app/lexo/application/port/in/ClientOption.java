package app.lexo.application.port.in;

import java.io.Serializable;

public record ClientOption(String id, String name) implements Serializable {
}
