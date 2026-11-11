package app.lexo.application.service;

import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.out.CarregarHonorariosDoClientePort;
import app.lexo.application.port.out.CarregarProcessosDoClientePort;
import app.lexo.common.erro.RecursoNaoEncontradoException;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.Cliente;
import app.lexo.domain.DadosDoCliente;
import app.lexo.domain.EscritorioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static app.lexo.application.service.ClientesEmMemoria.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConsultarPortalService")
class ConsultarPortalServiceTest {

    private static final EscritorioId ESCRITORIO = new EscritorioId("escritorio-1");

    private final ClientesEmMemoria clientes = new ClientesEmMemoria();
    private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

    private final List<ProcessoPortal> processos =
            List.of(new ProcessoPortal("P-1", "Civel", "ATIVO", Instant.now(), List.of(), List.of()));
    private final List<HonorarioPortal> honorarios = List.of(
            new HonorarioPortal("Consulta", new BigDecimal("100.00"), "PENDENTE", Instant.now()),
            new HonorarioPortal("Audiencia", new BigDecimal("50.50"), "ATRASADO", Instant.now()),
            new HonorarioPortal("Peticao", new BigDecimal("200.00"), "PAGO", Instant.now()));

    private String clienteComLink(long validadeEmDias) {
        Cliente cliente = clientes.salvar(Cliente.cadastrar(ESCRITORIO,
                new DadosDoCliente("Helena Prado", null, null, null, null)));
        String token = "a".repeat(64);
        cliente.gerarLinkDoPortal(token, AGORA, validadeEmDias);
        clientes.salvar(cliente);
        return token;
    }

    private ConsultarPortalService servico(CarregarProcessosDoClientePort processosPort,
                                           CarregarHonorariosDoClientePort honorariosPort) {
        return new ConsultarPortalService(clientes, processosPort, honorariosPort, relogio);
    }

    @Test
    @DisplayName("agrega processos e honorarios e soma so o que esta em aberto")
    void agrega() {
        String token = clienteComLink(90);

        PortalResponse resposta = servico((e, c) -> processos, (e, c) -> honorarios).consultar(token);

        assertThat(resposta.cliente()).isEqualTo("Helena Prado");
        assertThat(resposta.processos()).hasSize(1);
        assertThat(resposta.honorarios()).hasSize(3);
        assertThat(resposta.resumo().totalProcessos()).isEqualTo(1);
        assertThat(resposta.resumo().emAberto()).isEqualByComparingTo("150.50");
    }

    @Test
    @DisplayName("se um servico falha, a secao dele vem vazia e o resto continua")
    void degrada() {
        String token = clienteComLink(90);

        PortalResponse resposta = servico(
                (e, c) -> {
                    throw new IllegalStateException("fora do ar");
                },
                (e, c) -> honorarios).consultar(token);

        assertThat(resposta.processos()).isEmpty();
        assertThat(resposta.honorarios()).hasSize(3);
        assertThat(resposta.resumo().totalProcessos()).isZero();
    }

    @Test
    @DisplayName("token desconhecido devolve nao encontrado")
    void tokenInvalido() {
        clienteComLink(90);

        assertThatThrownBy(() -> servico((e, c) -> processos, (e, c) -> honorarios).consultar("outro"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Link inválido ou expirado");
    }

    @Test
    @DisplayName("link expirado devolve nao encontrado")
    void expirado() {
        String token = clienteComLink(-1);

        assertThatThrownBy(() -> servico((e, c) -> processos, (e, c) -> honorarios).consultar(token))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Link inválido ou expirado");
    }
}
