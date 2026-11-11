package app.lexo.adapter.out.persistence;

import app.lexo.IntegracaoBase;
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
import app.lexo.domain.CpfCnpj;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Adaptador de persistencia do cliente (integracao, Postgres)")
class ClientePersistenceAdapterIntegrationTest extends IntegracaoBase {

    @Autowired
    private SalvarClientePort salvar;

    @Autowired
    private CarregarClientePort carregar;

    @Autowired
    private ExcluirClientePort excluir;

    @Autowired
    private ListarClientesPort listar;

    @Autowired
    private ListarOpcoesDeClientePort opcoes;

    @Autowired
    private ContarClientesPort contar;

    @Autowired
    private ExisteClientePort existe;

    private EscritorioId novoEscritorio() {
        return new EscritorioId(UUID.randomUUID().toString());
    }

    private Cliente salvarCliente(EscritorioId escritorio, String nome, String documento, String email) {
        return salvar.salvar(Cliente.cadastrar(escritorio,
                new DadosDoCliente(nome, CpfCnpj.opcional(documento), email, null, null)));
    }

    @Test
    @DisplayName("salvar um cliente novo gera id e data de criacao e carrega de volta com todos os dados")
    void salvarNovo() {
        EscritorioId escritorio = novoEscritorio();

        Cliente salvo = salvar.salvar(Cliente.cadastrar(escritorio,
                new DadosDoCliente("Maria Souza", CpfCnpj.de("529.982.247-25"), "maria@x.com", "11999990000", "obs")));

        assertThat(salvo.id()).isNotNull();
        assertThat(salvo.criadoEm()).isNotNull();
        Cliente lido = carregar.porId(escritorio, salvo.id()).orElseThrow();
        assertThat(lido.dados()).isEqualTo(salvo.dados());
        assertThat(lido.dados().documento().texto()).isEqualTo("529.982.247-25");
        assertThat(lido.escritorioId()).isEqualTo(escritorio);
    }

    @Test
    @DisplayName("salvar um cliente existente atualiza a mesma linha e preserva a data de criacao")
    void salvarExistente() {
        EscritorioId escritorio = novoEscritorio();
        Cliente criado = carregar.porId(escritorio, salvarCliente(escritorio, "Maria Souza", null, null).id()).orElseThrow();

        criado.atualizar(new DadosDoCliente("Maria Souza Lima", null, "novo@x.com", null, null));
        Cliente atualizado = salvar.salvar(criado);

        assertThat(atualizado.id()).isEqualTo(criado.id());
        assertThat(atualizado.criadoEm()).isEqualTo(criado.criadoEm());
        assertThat(carregar.porId(escritorio, criado.id()).orElseThrow().nome()).isEqualTo("Maria Souza Lima");
        assertThat(contar.contar(escritorio)).isEqualTo(1);
    }

    @Test
    @DisplayName("carregar por id respeita o escritorio")
    void carregarPorEscritorio() {
        EscritorioId escritorio = novoEscritorio();
        Cliente salvo = salvarCliente(escritorio, "Maria Souza", null, null);

        assertThat(carregar.porId(novoEscritorio(), salvo.id())).isEmpty();
        assertThat(carregar.porId(escritorio, new ClienteId("inexistente"))).isEmpty();
        assertThat(existe.existe(escritorio, salvo.id())).isTrue();
        assertThat(existe.existe(novoEscritorio(), salvo.id())).isFalse();
    }

    @Test
    @DisplayName("o link do portal e salvo, encontrado pelo token e apagado ao revogar")
    void linkDoPortal() {
        EscritorioId escritorio = novoEscritorio();
        Cliente cliente = salvarCliente(escritorio, "Helena Prado", null, null);
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant agora = Instant.now();
        cliente.gerarLinkDoPortal(token, agora, 90);
        salvar.salvar(cliente);

        Cliente porToken = carregar.porLinkDoPortal(token).orElseThrow();
        assertThat(porToken.id()).isEqualTo(cliente.id());
        assertThat(porToken.portalAtivoEm(agora)).isTrue();

        porToken.revogarLinkDoPortal();
        salvar.salvar(porToken);
        assertThat(carregar.porLinkDoPortal(token)).isEmpty();
    }

    @Test
    @DisplayName("um documento antigo, fora da regra de hoje, ainda e lido do banco")
    void documentoAntigoELido() {
        EscritorioId escritorio = novoEscritorio();
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into clients (id, organization_id, name, document, created_at, updated_at) "
                + "values (?, ?, ?, ?, now(), now())", id, escritorio.valor(), "A", "documento antigo");

        Cliente lido = carregar.porId(escritorio, new ClienteId(id)).orElseThrow();

        assertThat(lido.nome()).isEqualTo("A");
        assertThat(lido.dados().documento().texto()).isEqualTo("documento antigo");
    }

    @Test
    @DisplayName("listar filtra por escritorio e por texto, ordena e pagina")
    void listar() {
        EscritorioId escritorio = novoEscritorio();
        salvarCliente(escritorio, "Carla Mendes", "529.982.247-25", "carla@x.com");
        salvarCliente(escritorio, "Ana Paula", null, "ANA@LOJA.COM");
        salvarCliente(escritorio, "Bruno Reis", null, null);
        salvarCliente(novoEscritorio(), "Outro Escritorio", null, null);

        Pagina<Cliente> todos = listar.listar(escritorio, null, new Paginacao(0, 20, List.of(new Ordem("name", true))));
        assertThat(todos.totalElements()).isEqualTo(3);
        assertThat(todos.content()).extracting(Cliente::nome).containsExactly("Ana Paula", "Bruno Reis", "Carla Mendes");

        Pagina<Cliente> decrescente = listar.listar(escritorio, "  ", new Paginacao(0, 20, List.of(new Ordem("name", false))));
        assertThat(decrescente.content().get(0).nome()).isEqualTo("Carla Mendes");

        Pagina<Cliente> segunda = listar.listar(escritorio, null, new Paginacao(1, 2, List.of(new Ordem("name", true))));
        assertThat(segunda.content()).hasSize(1);
        assertThat(segunda.totalPages()).isEqualTo(2);

        assertThat(listar.listar(escritorio, "ana@loja", new Paginacao(0, 20, List.of())).totalElements()).isEqualTo(1);
        assertThat(listar.listar(escritorio, "529.982", new Paginacao(0, 20, List.of())).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("opcoes saem em ordem alfabetica, so do escritorio")
    void opcoes() {
        EscritorioId escritorio = novoEscritorio();
        Cliente zeca = salvarCliente(escritorio, "Zeca Alves", null, null);
        Cliente ana = salvarCliente(escritorio, "Ana Paula", null, null);
        salvarCliente(novoEscritorio(), "Fora", null, null);

        List<ClienteOpcao> lista = opcoes.opcoes(escritorio);

        assertThat(lista).containsExactly(
                new ClienteOpcao(ana.id().valor(), "Ana Paula"), new ClienteOpcao(zeca.id().valor(), "Zeca Alves"));
    }

    @Test
    @DisplayName("excluir e salvar so alcancam clientes do proprio escritorio")
    void escopoDoEscritorio() {
        EscritorioId dono = novoEscritorio();
        EscritorioId invasor = novoEscritorio();
        Cliente cliente = salvarCliente(dono, "Carlos Dias", null, null);
        Cliente comoInvasor = Cliente.reconstituir(cliente.id(), invasor, cliente.dados(), null, cliente.criadoEm());

        excluir.excluir(comoInvasor);

        assertThat(carregar.porId(dono, cliente.id())).isPresent();
        assertThatThrownBy(() -> salvar.salvar(comoInvasor)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("excluir remove o cliente")
    void excluir() {
        EscritorioId escritorio = novoEscritorio();
        Cliente cliente = salvarCliente(escritorio, "Carlos Dias", null, null);

        excluir.excluir(cliente);

        assertThat(carregar.porId(escritorio, cliente.id())).isEmpty();
        assertThat(contar.contar(escritorio)).isZero();
    }
}
