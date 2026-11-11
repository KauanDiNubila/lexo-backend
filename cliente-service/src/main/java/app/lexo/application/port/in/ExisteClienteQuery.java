package app.lexo.application.port.in;

import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public interface ExisteClienteQuery {

    boolean existe(EscritorioId escritorioId, ClienteId clienteId);
}
