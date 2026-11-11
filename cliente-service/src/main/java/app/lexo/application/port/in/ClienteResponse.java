package app.lexo.application.port.in;

import app.lexo.domain.Cliente;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.LinkDoPortal;

import java.io.Serializable;
import java.time.Instant;

public record ClienteResponse(
        String id,
        String name,
        String document,
        String email,
        String phone,
        String notes,
        boolean portalAtivo,
        Instant portalExpiraEm,
        Instant createdAt
) implements Serializable {

    public static ClienteResponse de(Cliente cliente, Instant agora) {
        DadosDoCliente dados = cliente.dados();
        LinkDoPortal link = cliente.linkDoPortal();
        return new ClienteResponse(
                cliente.id().valor(),
                dados.nome(),
                dados.documento() == null ? null : dados.documento().texto(),
                dados.email(),
                dados.telefone(),
                dados.observacoes(),
                cliente.portalAtivoEm(agora),
                link == null ? null : link.expiraEm(),
                cliente.criadoEm());
    }
}
