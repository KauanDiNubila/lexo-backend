package app.lexo.application.port.out;

import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import java.util.Optional;

public interface CarregarClientePort {

    Optional<Cliente> porId(EscritorioId escritorioId, ClienteId clienteId);

    Optional<Cliente> porLinkDoPortal(String token);
}
