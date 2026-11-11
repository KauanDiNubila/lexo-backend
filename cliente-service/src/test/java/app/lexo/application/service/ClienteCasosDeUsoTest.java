package app.lexo.application.service;

import app.lexo.application.ConfiguracaoDoPortal;
import app.lexo.application.port.in.AtualizarClienteCommand;
import app.lexo.application.port.in.Autor;
import app.lexo.application.port.in.ClienteResponse;
import app.lexo.application.port.in.CriarClienteCommand;
import app.lexo.application.port.in.ExcluirClienteCommand;
import app.lexo.application.port.in.GerarLinkDoPortalCommand;
import app.lexo.application.port.in.LinkDoPortalResponse;
import app.lexo.application.port.in.RevogarLinkDoPortalCommand;
import app.lexo.application.port.out.EventoDeCliente;
import app.lexo.common.erro.DadoInvalidoException;
import app.lexo.domain.Cliente;
import app.lexo.domain.ClienteId;
import app.lexo.domain.ClienteNaoEncontradoException;
import app.lexo.domain.CpfCnpj;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static app.lexo.application.service.ClientesEmMemoria.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Casos de uso do cliente (portas simuladas em memoria)")
class ClienteCasosDeUsoTest {

    private static final EscritorioId ESCRITORIO = new EscritorioId("escritorio-1");
    private static final EscritorioId OUTRO_ESCRITORIO = new EscritorioId("escritorio-2");
    private static final Autor AUTOR = new Autor("usuario-1", "Ana Advogada");

    private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final ClientesEmMemoria clientes = new ClientesEmMemoria();
    private final EventosEmMemoria eventos = new EventosEmMemoria();

    private final CriarClienteService criar = new CriarClienteService(clientes, eventos, relogio);
    private final AtualizarClienteService atualizar = new AtualizarClienteService(clientes, clientes, relogio);
    private final ExcluirClienteService excluir = new ExcluirClienteService(clientes, clientes, eventos);
    private final GerarLinkDoPortalService gerarLink =
            new GerarLinkDoPortalService(clientes, clientes, relogio, new ConfiguracaoDoPortal(90));
    private final RevogarLinkDoPortalService revogarLink = new RevogarLinkDoPortalService(clientes, clientes);

    private DadosDoCliente dados(String nome, String documento) {
        return new DadosDoCliente(nome, CpfCnpj.opcional(documento), "maria@escritorio.com", "11999990000", null);
    }

    private ClienteResponse cadastrar(String nome) {
        return criar.criar(new CriarClienteCommand(ESCRITORIO, AUTOR, dados(nome, null)));
    }

    @Test
    @DisplayName("criar salva o cliente, publica CLIENTE_CRIADO e devolve a resposta")
    void criar() {
        ClienteResponse resposta = criar.criar(
                new CriarClienteCommand(ESCRITORIO, AUTOR, dados("Maria Souza", "529.982.247-25")));

        assertThat(resposta.id()).isNotBlank();
        assertThat(resposta.name()).isEqualTo("Maria Souza");
        assertThat(resposta.document()).isEqualTo("529.982.247-25");
        assertThat(resposta.portalAtivo()).isFalse();
        assertThat(resposta.createdAt()).isEqualTo(AGORA);
        assertThat(clientes.todos()).hasSize(1);
        assertThat(eventos.publicados).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo(EventoDeCliente.CRIADO);
            assertThat(evento.escritorioId()).isEqualTo(ESCRITORIO);
            assertThat(evento.clienteId().valor()).isEqualTo(resposta.id());
            assertThat(evento.descricao()).isEqualTo("Cliente criado: Maria Souza");
            assertThat(evento.autor()).isEqualTo(AUTOR);
        });
    }

    @Test
    @DisplayName("nome curto e recusado pelo proprio comando, sem salvar nem publicar")
    void nomeCurto() {
        assertThatThrownBy(() -> new CriarClienteCommand(ESCRITORIO, AUTOR, dados("A", null)))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("Nome muito curto");
        assertThatThrownBy(() -> new AtualizarClienteCommand(ESCRITORIO, new ClienteId("c1"), dados("  ", null)))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(clientes.todos()).isEmpty();
        assertThat(eventos.publicados).isEmpty();
    }

    @Test
    @DisplayName("atualizar troca os dados, nao publica evento e devolve a resposta")
    void atualizar() {
        ClienteResponse criado = cadastrar("Maria Souza");

        ClienteResponse atualizado = atualizar.atualizar(new AtualizarClienteCommand(
                ESCRITORIO, new ClienteId(criado.id()), dados("Maria Souza Lima", null)));

        assertThat(atualizado.id()).isEqualTo(criado.id());
        assertThat(atualizado.name()).isEqualTo("Maria Souza Lima");
        assertThat(eventos.publicados).hasSize(1);
    }

    @Test
    @DisplayName("atualizar cliente de outro escritorio devolve nao encontrado")
    void atualizarDeOutroEscritorio() {
        ClienteResponse criado = cadastrar("Maria Souza");

        assertThatThrownBy(() -> atualizar.atualizar(new AtualizarClienteCommand(
                OUTRO_ESCRITORIO, new ClienteId(criado.id()), dados("Invasor", null))))
                .isInstanceOf(ClienteNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    @DisplayName("excluir remove o cliente e publica CLIENTE_EXCLUIDO")
    void excluir() {
        ClienteResponse criado = cadastrar("Carlos Dias");

        excluir.excluir(new ExcluirClienteCommand(ESCRITORIO, new ClienteId(criado.id()), AUTOR));

        assertThat(clientes.todos()).isEmpty();
        assertThat(eventos.publicados).last().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo(EventoDeCliente.EXCLUIDO);
            assertThat(evento.descricao()).isEqualTo("Cliente excluído: Carlos Dias");
        });
    }

    @Test
    @DisplayName("excluir cliente de outro escritorio nao exclui nem publica")
    void excluirDeOutroEscritorio() {
        ClienteResponse criado = cadastrar("Carlos Dias");

        assertThatThrownBy(() -> excluir.excluir(
                new ExcluirClienteCommand(OUTRO_ESCRITORIO, new ClienteId(criado.id()), AUTOR)))
                .isInstanceOf(ClienteNaoEncontradoException.class);

        assertThat(clientes.todos()).hasSize(1);
        assertThat(eventos.publicados).hasSize(1);
    }

    @Test
    @DisplayName("gerar link cria um token de 64 caracteres valido por 90 dias")
    void gerarLink() {
        ClienteResponse criado = cadastrar("Helena Prado");

        LinkDoPortalResponse link = gerarLink.gerar(new GerarLinkDoPortalCommand(ESCRITORIO, new ClienteId(criado.id())));

        assertThat(link.token()).hasSize(64);
        Cliente salvo = clientes.porLinkDoPortal(link.token()).orElseThrow();
        assertThat(salvo.linkDoPortal().expiraEm()).isEqualTo(Instant.parse("2027-02-09T12:00:00Z"));
        assertThat(salvo.portalAtivoEm(AGORA)).isTrue();
    }

    @Test
    @DisplayName("um novo link invalida o anterior")
    void novoLinkInvalidaOAnterior() {
        ClienteResponse criado = cadastrar("Helena Prado");
        GerarLinkDoPortalCommand comando = new GerarLinkDoPortalCommand(ESCRITORIO, new ClienteId(criado.id()));

        String primeiro = gerarLink.gerar(comando).token();
        String segundo = gerarLink.gerar(comando).token();

        assertThat(segundo).isNotEqualTo(primeiro);
        assertThat(clientes.porLinkDoPortal(primeiro)).isEmpty();
        assertThat(clientes.porLinkDoPortal(segundo)).isPresent();
    }

    @Test
    @DisplayName("revogar apaga o link")
    void revogarLink() {
        ClienteResponse criado = cadastrar("Helena Prado");
        ClienteId id = new ClienteId(criado.id());
        String token = gerarLink.gerar(new GerarLinkDoPortalCommand(ESCRITORIO, id)).token();

        revogarLink.revogar(new RevogarLinkDoPortalCommand(ESCRITORIO, id));

        assertThat(clientes.porLinkDoPortal(token)).isEmpty();
    }

    @Test
    @DisplayName("gerar e revogar link de cliente de outro escritorio devolve nao encontrado")
    void linkDeOutroEscritorio() {
        ClienteResponse criado = cadastrar("Helena Prado");
        ClienteId id = new ClienteId(criado.id());

        assertThatThrownBy(() -> gerarLink.gerar(new GerarLinkDoPortalCommand(OUTRO_ESCRITORIO, id)))
                .isInstanceOf(ClienteNaoEncontradoException.class);
        assertThatThrownBy(() -> revogarLink.revogar(new RevogarLinkDoPortalCommand(OUTRO_ESCRITORIO, id)))
                .isInstanceOf(ClienteNaoEncontradoException.class);
    }
}
