package app.lexo.application.port.in;

import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public interface BuscarClienteQuery {

    ClienteResponse buscar(EscritorioId escritorioId, ClienteId clienteId);
}
