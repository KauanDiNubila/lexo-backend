package app.lexo.adapter.out.persistence;

import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.CpfCnpj;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;
import app.lexo.domain.LinkDoPortal;

final class ClienteMapper {

    private ClienteMapper() {
    }

    static Cliente paraDominio(ClienteJpaEntity entidade) {
        DadosDoCliente dados = new DadosDoCliente(entidade.name, CpfCnpj.armazenado(entidade.document),
                entidade.email, entidade.phone, entidade.notes);
        LinkDoPortal link = entidade.portalToken == null
                ? null
                : new LinkDoPortal(entidade.portalToken, entidade.portalTokenExpiresAt);
        return Cliente.reconstituir(new ClienteId(entidade.getId()), new EscritorioId(entidade.organizationId),
                dados, link, entidade.getCreatedAt());
    }

    static void copiar(Cliente cliente, ClienteJpaEntity entidade) {
        DadosDoCliente dados = cliente.dados();
        entidade.organizationId = cliente.escritorioId().valor();
        entidade.name = dados.nome();
        entidade.document = dados.documento() == null ? null : dados.documento().texto();
        entidade.email = dados.email();
        entidade.phone = dados.telefone();
        entidade.notes = dados.observacoes();
        LinkDoPortal link = cliente.linkDoPortal();
        entidade.portalToken = link == null ? null : link.token();
        entidade.portalTokenExpiresAt = link == null ? null : link.expiraEm();
    }
}
