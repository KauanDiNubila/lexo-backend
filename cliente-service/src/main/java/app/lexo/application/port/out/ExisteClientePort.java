package app.lexo.application.port.out;

import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public interface ExisteClientePort {

    boolean existe(EscritorioId escritorioId, ClienteId clienteId);
}
