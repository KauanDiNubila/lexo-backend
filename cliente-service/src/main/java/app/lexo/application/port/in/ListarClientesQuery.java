package app.lexo.application.port.in;

import app.lexo.common.web.Pagina;
import app.lexo.domain.EscritorioId;

public interface ListarClientesQuery {

    Pagina<ClienteResponse> listar(EscritorioId escritorioId, String busca, Paginacao paginacao);
}
