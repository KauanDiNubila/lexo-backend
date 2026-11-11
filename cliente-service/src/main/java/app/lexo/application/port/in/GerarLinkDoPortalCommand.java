package app.lexo.application.port.in;

import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public record GerarLinkDoPortalCommand(EscritorioId escritorioId, ClienteId clienteId) {
}
