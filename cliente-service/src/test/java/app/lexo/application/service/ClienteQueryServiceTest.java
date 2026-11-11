package app.lexo.application.service;

import app.lexo.application.port.in.ClienteOpcao;
import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.in.Ordem;
import app.lexo.application.port.in.Paginacao;
import app.lexo.application.port.in.ResumoClientes;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.ClienteNaoEncontradoException;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static app.lexo.application.service.ClientesEmMemoria.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClienteQueryService")
class ClienteQueryServiceTest {

    private static final EscritorioId ESCRITORIO = new EscritorioId("escritorio-1");
    private static final EscritorioId OUTRO_ESCRITORIO = new EscritorioId("escritorio-2");

    private final ClientesEmMemoria clientes = new ClientesEmMemoria();
    private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

    private Cliente salvar(EscritorioId escritorio, String nome) {
        return clientes.salvar(Cliente.cadastrar(escritorio, new DadosDoCliente(nome, null, null, null, null)));
    }

    @Test
    @DisplayName("buscar devolve o cliente do escritorio e nao encontrado para outro escritorio")
    void buscar() {
        Cliente maria = salvar(ESCRITORIO, "Maria Souza");
        ClienteQueryService servico = new ClienteQueryService(clientes, null, null, null, null, relogio);

        ClienteResponse resposta = servico.buscar(ESCRITORIO, maria.id());

        assertThat(resposta.name()).isEqualTo("Maria Souza");
        assertThatThrownBy(() -> servico.buscar(OUTRO_ESCRITORIO, maria.id()))
                .isInstanceOf(ClienteNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.buscar(ESCRITORIO, new ClienteId("inexistente")))
                .isInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    @DisplayName("listar converte a pagina de dominio em respostas, mantendo os totais")
    void listar() {
        Cliente ana = salvar(ESCRITORIO, "Ana Paula");
        Paginacao paginacao = new Paginacao(0, 20, List.of(new Ordem("name", true)));
        ClienteQueryService servico = new ClienteQueryService(clientes,
                (escritorio, busca, pedido) -> new Pagina<>(List.of(ana), 0, 20, 1, 1),
                null, null, null, relogio);

        Pagina<ClienteResponse> pagina = servico.listar(ESCRITORIO, "ana", paginacao);

        assertThat(pagina.content()).extracting(ClienteResponse::name).containsExactly("Ana Paula");
        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.totalPages()).isEqualTo(1);
        assertThat(pagina.page()).isZero();
    }

    @Test
    @DisplayName("opcoes, resumo e existe delegam para as portas de saida")
    void delegacoes() {
        ClienteQueryService servico = new ClienteQueryService(clientes, null,
                escritorio -> List.of(new ClienteOpcao("c1", "Ana")),
                escritorio -> 7L,
                (escritorio, id) -> id.valor().equals("c1"),
                relogio);

        assertThat(servico.listarOpcoes(ESCRITORIO)).containsExactly(new ClienteOpcao("c1", "Ana"));
        assertThat(servico.resumir(ESCRITORIO)).isEqualTo(new ResumoClientes(7));
        assertThat(servico.existe(ESCRITORIO, new ClienteId("c1"))).isTrue();
        assertThat(servico.existe(ESCRITORIO, new ClienteId("c2"))).isFalse();
    }
}
