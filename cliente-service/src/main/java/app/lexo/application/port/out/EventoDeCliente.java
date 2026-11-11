package app.lexo.application.port.out;

import app.lexo.application.port.in.Autor;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

public record EventoDeCliente(String tipo, EscritorioId escritorioId, ClienteId clienteId, String descricao,
                              Autor autor) {

    public static final String CRIADO = "CLIENTE_CRIADO";
    public static final String EXCLUIDO = "CLIENTE_EXCLUIDO";

    public static EventoDeCliente criado(Cliente cliente, Autor autor) {
        return new EventoDeCliente(CRIADO, cliente.escritorioId(), cliente.id(),
                "Cliente criado: " + cliente.nome(), autor);
    }

    public static EventoDeCliente excluido(Cliente cliente, Autor autor) {
        return new EventoDeCliente(EXCLUIDO, cliente.escritorioId(), cliente.id(),
                "Cliente excluído: " + cliente.nome(), autor);
    }
}
