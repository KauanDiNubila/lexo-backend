package app.lexo.application.port.in;

import app.lexo.domain.EscritorioId;

public interface ResumoDeClientesQuery {

    ResumoClientes resumir(EscritorioId escritorioId);
}
