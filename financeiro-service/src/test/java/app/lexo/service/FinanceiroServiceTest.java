package app.lexo.service;

import app.lexo.client.ReferenciaGateway;
import app.lexo.common.security.AuthUser;
import app.lexo.common.security.Role;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Invoice;
import app.lexo.domain.enums.InvoiceStatus;
import app.lexo.dto.InvoiceDtos.FinancialReport;
import app.lexo.dto.InvoiceDtos.InvoiceRequest;
import app.lexo.dto.InvoiceDtos.InvoiceResponse;
import app.lexo.dto.InvoiceDtos.ResumoFinanceiro;
import app.lexo.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinanceiroService (unitario)")
class FinanceiroServiceTest {

    private static final String ORG = "org-123";
    private static final String OUTRA_ORG = "org-999";
    private static final String CLIENTE = "cliente-1";
    private static final String PROCESSO = "processo-1";

    @Mock
    private InvoiceRepository repo;

    @Mock
    private ReferenciaGateway referencias;

    @InjectMocks
    private FinanceiroService service;

    private AuthUser usuario(String org) {
        return new AuthUser("user-1", org, "Ana Advogada", "ana@escritorio.com", Role.ADMIN);
    }

    private Invoice honorario(String id, String org, BigDecimal valor, InvoiceStatus status) {
        Invoice i = new Invoice();
        i.setId(id);
        i.setOrganizationId(org);
        i.setClientId(CLIENTE);
        i.setDescription("Honorarios contratuais");
        i.setAmount(valor);
        i.setStatus(status);
        i.setDueDate(Instant.now().plus(30, ChronoUnit.DAYS));
        return i;
    }

    private InvoiceRequest requisicao(BigDecimal valor, InvoiceStatus status) {
        return requisicao(valor, status, null);
    }

    private InvoiceRequest requisicao(BigDecimal valor, InvoiceStatus status, String caseId) {
        return new InvoiceRequest(CLIENTE, caseId, "Honorarios contratuais", valor, status,
                Instant.now().plus(30, ChronoUnit.DAYS));
    }

    private void referenciasValidas() {
        lenient().when(referencias.clienteExiste(eq(CLIENTE), any())).thenReturn(true);
        lenient().when(referencias.processoExiste(eq(PROCESSO), any())).thenReturn(true);
    }

    @Test
    @DisplayName("listar devolve a pagina filtrada pela organizacao do usuario")
    void listarFiltraPelaOrganizacao() {
        Pageable pagina = PageRequest.of(0, 20);
        when(repo.findByOrganizationId(ORG, pagina)).thenReturn(new PageImpl<>(
                List.of(honorario("i1", ORG, new BigDecimal("1000.00"), InvoiceStatus.PENDENTE),
                        honorario("i2", ORG, new BigDecimal("500.00"), InvoiceStatus.PAGO)),
                pagina, 2));

        Pagina<InvoiceResponse> resultado = service.listar(usuario(ORG), pagina);

        assertThat(resultado.content()).hasSize(2);
        assertThat(resultado.totalElements()).isEqualTo(2);
        assertThat(resultado.content()).extracting(InvoiceResponse::id).containsExactly("i1", "i2");
    }

    @Test
    @DisplayName("resumo separa em aberto (pendente + atrasado) de recebido (pago)")
    void resumoSeparaEmAbertoDeRecebido() {
        when(repo.somarPorStatus(eq(ORG), eq(Set.of(InvoiceStatus.PENDENTE, InvoiceStatus.ATRASADO))))
                .thenReturn(new BigDecimal("1500.00"));
        when(repo.somarPorStatus(eq(ORG), eq(Set.of(InvoiceStatus.PAGO))))
                .thenReturn(new BigDecimal("800.00"));

        ResumoFinanceiro resumo = service.resumo(usuario(ORG));

        assertThat(resumo.emAberto()).isEqualByComparingTo("1500.00");
        assertThat(resumo.recebido()).isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("criar carimba a organizacao do usuario no honorario")
    void criarCarimbaOrganizacao() {
        referenciasValidas();
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId("novo-id");
            return i;
        });

        InvoiceResponse resposta = service.criar(usuario(ORG),
                requisicao(new BigDecimal("2500.00"), InvoiceStatus.PENDENTE));

        assertThat(resposta.id()).isEqualTo("novo-id");

        ArgumentCaptor<Invoice> salvo = ArgumentCaptor.forClass(Invoice.class);
        verify(repo).save(salvo.capture());
        assertThat(salvo.getValue().getOrganizationId()).isEqualTo(ORG);
        assertThat(salvo.getValue().getAmount()).isEqualByComparingTo("2500.00");
    }

    @Test
    @DisplayName("criar como PAGO ja marca a data de pagamento")
    void criarPagoMarcaPaidAt() {
        referenciasValidas();
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse resposta = service.criar(usuario(ORG),
                requisicao(new BigDecimal("300.00"), InvoiceStatus.PAGO));

        assertThat(resposta.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("criar como PENDENTE nao marca data de pagamento")
    void criarPendenteNaoMarcaPaidAt() {
        referenciasValidas();
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse resposta = service.criar(usuario(ORG),
                requisicao(new BigDecimal("300.00"), InvoiceStatus.PENDENTE));

        assertThat(resposta.paidAt()).isNull();
    }

    @Test
    @DisplayName("criar com cliente inexistente lanca 404 e nao salva")
    void criarComClienteInexistenteLanca404() {
        when(referencias.clienteExiste(CLIENTE, ORG)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(usuario(ORG),
                requisicao(new BigDecimal("100.00"), InvoiceStatus.PENDENTE)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("criar com processo inexistente lanca 404 e nao salva")
    void criarComProcessoInexistenteLanca404() {
        when(referencias.clienteExiste(CLIENTE, ORG)).thenReturn(true);
        when(referencias.processoExiste(PROCESSO, ORG)).thenReturn(false);

        assertThatThrownBy(() -> service.criar(usuario(ORG),
                requisicao(new BigDecimal("100.00"), InvoiceStatus.PENDENTE, PROCESSO)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("criar sem processo nao consulta o processo-service")
    void criarSemProcessoNaoValidaProcesso() {
        when(referencias.clienteExiste(CLIENTE, ORG)).thenReturn(true);
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(usuario(ORG), requisicao(new BigDecimal("100.00"), InvoiceStatus.PENDENTE));

        verify(referencias, never()).processoExiste(any(), any());
    }

    @Test
    @DisplayName("atualizar altera os campos quando o honorario existe na organizacao")
    void atualizarQuandoExiste() {
        referenciasValidas();
        Invoice existente = honorario("i1", ORG, new BigDecimal("100.00"), InvoiceStatus.PENDENTE);
        when(repo.findByIdAndOrganizationId("i1", ORG)).thenReturn(Optional.of(existente));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse resposta = service.atualizar(usuario(ORG), "i1",
                requisicao(new BigDecimal("999.00"), InvoiceStatus.PENDENTE));

        assertThat(resposta.amount()).isEqualByComparingTo("999.00");
        assertThat(existente.getAmount()).isEqualByComparingTo("999.00");
    }

    @Test
    @DisplayName("atualizar de PAGO para PENDENTE limpa a data de pagamento")
    void atualizarDePagoParaPendenteLimpaPaidAt() {
        referenciasValidas();
        Invoice existente = honorario("i1", ORG, new BigDecimal("100.00"), InvoiceStatus.PAGO);
        existente.setPaidAt(Instant.now());
        when(repo.findByIdAndOrganizationId("i1", ORG)).thenReturn(Optional.of(existente));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse resposta = service.atualizar(usuario(ORG), "i1",
                requisicao(new BigDecimal("100.00"), InvoiceStatus.PENDENTE));

        assertThat(resposta.paidAt()).isNull();
    }

    @Test
    @DisplayName("atualizar lanca 404 quando o honorario nao existe na organizacao")
    void atualizarQuandoNaoExisteLanca404() {
        referenciasValidas();
        when(repo.findByIdAndOrganizationId("i1", ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(usuario(ORG), "i1",
                requisicao(new BigDecimal("100.00"), InvoiceStatus.PENDENTE)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("atualizarStatus para PAGO marca a data de pagamento")
    void atualizarStatusParaPagoMarcaPaidAt() {
        Invoice existente = honorario("i1", ORG, new BigDecimal("100.00"), InvoiceStatus.PENDENTE);
        when(repo.findByIdAndOrganizationId("i1", ORG)).thenReturn(Optional.of(existente));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse resposta = service.atualizarStatus(usuario(ORG), "i1", InvoiceStatus.PAGO);

        assertThat(resposta.status()).isEqualTo(InvoiceStatus.PAGO);
        assertThat(resposta.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("atualizarStatus nao consulta cliente nem processo")
    void atualizarStatusNaoValidaReferencias() {
        when(repo.findByIdAndOrganizationId("i1", ORG))
                .thenReturn(Optional.of(honorario("i1", ORG, new BigDecimal("100.00"), InvoiceStatus.PENDENTE)));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarStatus(usuario(ORG), "i1", InvoiceStatus.CANCELADO);

        verify(referencias, never()).clienteExiste(any(), any());
        verify(referencias, never()).processoExiste(any(), any());
    }

    @Test
    @DisplayName("excluir so enxerga honorario da organizacao do usuario (multi-tenant)")
    void excluirRestringePelaOrganizacao() {
        when(repo.findByIdAndOrganizationId("i1", OUTRA_ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(usuario(OUTRA_ORG), "i1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(repo, never()).delete(any(Invoice.class));
    }

    @Test
    @DisplayName("resumo nao vaza valores de outra organizacao")
    void resumoIsolaPorOrganizacao() {
        when(repo.somarPorStatus(eq(OUTRA_ORG), anySet())).thenReturn(BigDecimal.ZERO);

        ResumoFinanceiro resumo = service.resumo(usuario(OUTRA_ORG));

        assertThat(resumo.emAberto()).isEqualByComparingTo("0");
        assertThat(resumo.recebido()).isEqualByComparingTo("0");
        verify(repo, never()).somarPorStatus(eq(ORG), anySet());
    }

    @Test
    @DisplayName("relatorio soma os honorarios do periodo separados por status")
    void relatorioSomaPorStatus() {
        Instant inicio = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant fim = Instant.now().plus(30, ChronoUnit.DAYS);
        when(repo.findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(ORG, inicio, fim))
                .thenReturn(List.of(
                        honorario("i1", ORG, new BigDecimal("100.00"), InvoiceStatus.PAGO),
                        honorario("i2", ORG, new BigDecimal("250.00"), InvoiceStatus.PAGO),
                        honorario("i3", ORG, new BigDecimal("400.00"), InvoiceStatus.PENDENTE),
                        honorario("i4", ORG, new BigDecimal("50.00"), InvoiceStatus.ATRASADO),
                        honorario("i5", ORG, new BigDecimal("999.00"), InvoiceStatus.CANCELADO)));

        FinancialReport relatorio = service.relatorio(usuario(ORG), inicio, fim);

        assertThat(relatorio.totalPago()).isEqualByComparingTo("350.00");
        assertThat(relatorio.totalPendente()).isEqualByComparingTo("400.00");
        assertThat(relatorio.totalAtrasado()).isEqualByComparingTo("50.00");
        assertThat(relatorio.start()).isEqualTo(inicio);
        assertThat(relatorio.end()).isEqualTo(fim);
    }

    @Test
    @DisplayName("relatorio de periodo vazio devolve zeros, nao nulos")
    void relatorioVazioDevolveZeros() {
        Instant inicio = Instant.now();
        Instant fim = inicio.plus(1, ChronoUnit.DAYS);
        when(repo.findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(ORG, inicio, fim))
                .thenReturn(List.of());

        FinancialReport relatorio = service.relatorio(usuario(ORG), inicio, fim);

        assertThat(relatorio.totalPago()).isEqualByComparingTo("0");
        assertThat(relatorio.totalPendente()).isEqualByComparingTo("0");
        assertThat(relatorio.totalAtrasado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("excluirPorCliente apaga em cascata os honorarios do cliente na organizacao")
    void excluirPorClienteCascata() {
        when(repo.deleteByClientIdAndOrganizationId(CLIENTE, ORG)).thenReturn(3L);

        int apagados = service.excluirPorCliente(ORG, CLIENTE);

        assertThat(apagados).isEqualTo(3);
        verify(repo).deleteByClientIdAndOrganizationId(CLIENTE, ORG);
    }

    @Test
    @DisplayName("desvincularProcesso solta os honorarios sem apagar nada")
    void desvincularProcessoNaoApaga() {
        when(repo.desvincularProcesso(PROCESSO, ORG)).thenReturn(2);

        int afetados = service.desvincularProcesso(ORG, PROCESSO);

        assertThat(afetados).isEqualTo(2);
        verify(repo).desvincularProcesso(PROCESSO, ORG);
        verify(repo, never()).delete(any(Invoice.class));
    }
}
