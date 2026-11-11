package app.lexo.application.port.in;

import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public record RevogarLinkDoPortalCommand(EscritorioId escritorioId, ClienteId clienteId) {
}
