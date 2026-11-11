package app.lexo.application.port.in;

import app.lexo.domain.ClienteId;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;

public record AtualizarClienteCommand(EscritorioId escritorioId, ClienteId clienteId, DadosDoCliente dados) {

    public AtualizarClienteCommand {
        NomeDoCliente.exigirValido(dados.nome());
    }
}
