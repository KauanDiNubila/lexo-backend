package app.lexo.application.port.in;

import app.lexo.domain.EscritorioId;
import java.util.List;

public interface ListarOpcoesDeClienteQuery {

    List<ClienteOpcao> listar(EscritorioId escritorioId);
}
