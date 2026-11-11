package app.lexo.adapter.out.persistence;

import app.lexo.application.port.in.ClienteOpcao;
import app.lexo.application.port.in.Ordem;
import app.lexo.application.port.in.Paginacao;
import app.lexo.application.port.out.CarregarClientePort;
import app.lexo.application.port.out.ContarClientesPort;
import app.lexo.application.port.out.ExcluirClientePort;
import app.lexo.application.port.out.ExisteClientePort;
import app.lexo.application.port.out.ListarClientesPort;
import app.lexo.application.port.out.ListarOpcoesDeClientePort;
import app.lexo.application.port.out.SalvarClientePort;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.EscritorioId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class ClientePersistenceAdapter implements SalvarClientePort, CarregarClientePort, ExcluirClientePort,
        ListarClientesPort, ListarOpcoesDeClientePort, ContarClientesPort, ExisteClientePort {

    private static final String CACHE_OPCOES = "clientes-opcoes-v2";

    private final SpringDataClienteRepository repositorio;

    ClientePersistenceAdapter(SpringDataClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @CacheEvict(value = CACHE_OPCOES, key = "#cliente.escritorioId().valor()")
    public Cliente salvar(Cliente cliente) {
        ClienteJpaEntity entidade = cliente.id() == null
                ? new ClienteJpaEntity()
                : repositorio.findById(cliente.id().valor()).orElseThrow(
                        () -> new IllegalStateException("Cliente " + cliente.id().valor() + " nao existe para atualizar"));
        ClienteMapper.copiar(cliente, entidade);
        return ClienteMapper.paraDominio(repositorio.save(entidade));
    }

    @Override
    public Optional<Cliente> porId(EscritorioId escritorioId, ClienteId clienteId) {
        return repositorio.findByIdAndOrganizationId(clienteId.valor(), escritorioId.valor())
                .map(ClienteMapper::paraDominio);
    }

    @Override
    public Optional<Cliente> porLinkDoPortal(String token) {
        return repositorio.findByPortalToken(token).map(ClienteMapper::paraDominio);
    }

    @Override
    @CacheEvict(value = CACHE_OPCOES, key = "#cliente.escritorioId().valor()")
    public void excluir(Cliente cliente) {
        repositorio.deleteById(cliente.id().valor());
    }

    @Override
    public Pagina<Cliente> listar(EscritorioId escritorioId, String busca, Paginacao paginacao) {
        Specification<ClienteJpaEntity> filtro =
                ClienteSpecs.doEscritorio(escritorioId.valor()).and(ClienteSpecs.busca(busca));
        return Pagina.de(repositorio.findAll(filtro, paraPageRequest(paginacao)), ClienteMapper::paraDominio);
    }

    @Override
    @Cacheable(value = CACHE_OPCOES, key = "#escritorioId.valor()")
    public List<ClienteOpcao> opcoes(EscritorioId escritorioId) {
        return repositorio.findByOrganizationIdOrderByNameAsc(escritorioId.valor()).stream()
                .map(entidade -> new ClienteOpcao(entidade.getId(), entidade.name))
                .toList();
    }

    @Override
    public long contar(EscritorioId escritorioId) {
        return repositorio.countByOrganizationId(escritorioId.valor());
    }

    @Override
    public boolean existe(EscritorioId escritorioId, ClienteId clienteId) {
        return repositorio.existsByIdAndOrganizationId(clienteId.valor(), escritorioId.valor());
    }

    private PageRequest paraPageRequest(Paginacao paginacao) {
        List<Sort.Order> ordens = paginacao.ordens().stream().map(this::paraOrdem).toList();
        return PageRequest.of(paginacao.pagina(), paginacao.tamanho(), Sort.by(ordens));
    }

    private Sort.Order paraOrdem(Ordem ordem) {
        return ordem.ascendente() ? Sort.Order.asc(ordem.propriedade()) : Sort.Order.desc(ordem.propriedade());
    }
}
