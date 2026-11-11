package app.lexo.application.service;

import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.ExcluirClientePort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ClientesEmMemoria implements SalvarClientePort, CarregarClientePort, ExcluirClientePort {

    static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");

    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private int sequencia;

    @Override
    public Cliente salvar(Cliente cliente) {
        Cliente salvo = cliente.id() == null
                ? Cliente.reconstituir(new ClienteId("c" + (++sequencia)), cliente.escritorioId(), cliente.dados(),
                        cliente.linkDoPortal(), AGORA)
                : cliente;
        clientes.put(salvo.id().valor(), salvo);
        return salvo;
    }

    @Override
    public Optional<Cliente> porId(EscritorioId escritorioId, ClienteId clienteId) {
        return Optional.ofNullable(clientes.get(clienteId.valor()))
                .filter(c -> c.escritorioId().equals(escritorioId));
    }

    @Override
    public Optional<Cliente> porLinkDoPortal(String token) {
        return clientes.values().stream()
                .filter(c -> c.linkDoPortal() != null && c.linkDoPortal().token().equals(token))
                .findFirst();
    }

    @Override
    public void excluir(Cliente cliente) {
        clientes.remove(cliente.id().valor());
    }

    List<Cliente> todos() {
        return new ArrayList<>(clientes.values());
    }
}
