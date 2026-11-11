package app.lexo.application.port.in;

import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;

public record CriarClienteCommand(EscritorioId escritorioId, Autor autor, DadosDoCliente dados) {

    public CriarClienteCommand {
        NomeDoCliente.exigirValido(dados.nome());
    }
}
