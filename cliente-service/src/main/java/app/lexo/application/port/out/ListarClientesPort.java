package app.lexo.application.port.out;

import app.lexo.application.port.in.Paginacao;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Cliente;
import app.lexo.domain.EscritorioId;

public interface ListarClientesPort {

    Pagina<Cliente> listar(EscritorioId escritorioId, String busca, Paginacao paginacao);
}
