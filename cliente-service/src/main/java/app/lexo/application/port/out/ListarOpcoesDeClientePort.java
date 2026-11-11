package app.lexo.application.port.out;

import app.lexo.application.port.in.ClienteOpcao;
import app.lexo.domain.EscritorioId;
import java.util.List;

public interface ListarOpcoesDeClientePort {

    List<ClienteOpcao> opcoes(EscritorioId escritorioId);
}
