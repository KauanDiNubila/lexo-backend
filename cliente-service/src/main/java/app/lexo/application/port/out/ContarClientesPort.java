package app.lexo.application.port.out;

import app.lexo.domain.EscritorioId;

public interface ContarClientesPort {

    long contar(EscritorioId escritorioId);
}
