package app.lexo.service;

import app.lexo.client.FinanceiroPortalClient;
import app.lexo.client.ProcessoPortalClient;
import app.lexo.controller.ApiException;
import app.lexo.domain.Client;
import app.lexo.dto.PortalDtos.HonorarioPortal;
import app.lexo.dto.PortalDtos.PortalResponse;
import app.lexo.dto.PortalDtos.ProcessoPortal;
import app.lexo.repository.ClientRepository;
import app.lexo.security.AuthUser;
import app.lexo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalService (unitario)")
class PortalServiceTest {

    private static final String ORG = "org-123";
    private static final String TOKEN = "token-abc";

    @Mock
    private ClientRepository repo;

    @Mock
    private ProcessoPortalClient processoClient;

    @Mock
    private FinanceiroPortalClient financeiroClient;

    @InjectMocks
    private PortalService service;

    private AuthUser usuario() {
        return new AuthUser("user-1", ORG, "Ana", "ana@x.com", Role.ADMIN);
    }

    private Client clienteComToken() {
        Client c = new Client();
        c.setId("c1");
        c.setOrganizationId(ORG);
        c.setName("Cliente Portal");
        c.setPortalToken(TOKEN);
        return c;
    }

    private HonorarioPortal honorario(String status, String valor) {
        return new HonorarioPortal("Honorario", new BigDecimal(valor), status, Instant.now());
    }

    @Test
    @DisplayName("gerarToken cria um token e persiste o cliente")
    void gerarTokenPersiste() {
        Client c = new Client();
        c.setId("c1");
        c.setOrganizationId(ORG);
        c.setName("Cliente");
        when(repo.findByIdAndOrganizationId("c1", ORG)).thenReturn(Optional.of(c));

        String token = service.gerarToken(usuario(), "c1");

        assertThat(token).isNotBlank();
        assertThat(c.getPortalToken()).isEqualTo(token);
        verify(repo).save(c);
    }

    @Test
    @DisplayName("gerarToken lanca 404 quando o cliente nao existe na organizacao")
    void gerarTokenClienteInexistente() {
        when(repo.findByIdAndOrganizationId("c1", ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarToken(usuario(), "c1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("porToken agrega processos e honorarios e soma apenas o que esta em aberto")
    void porTokenAgregaESoma() {
        when(repo.findByPortalToken(TOKEN)).thenReturn(Optional.of(clienteComToken()));
        when(processoClient.processosDoCliente("c1", ORG)).thenReturn(List.of(
                new ProcessoPortal("0001", "Civel", "ATIVO", Instant.now(), List.of(), List.of())));
        when(financeiroClient.honorariosDoCliente("c1", ORG)).thenReturn(List.of(
                honorario("PENDENTE", "100.00"),
                honorario("PAGO", "50.00"),
                honorario("ATRASADO", "30.00")));

        PortalResponse resp = service.porToken(TOKEN);

        assertThat(resp.cliente()).isEqualTo("Cliente Portal");
        assertThat(resp.processos()).hasSize(1);
        assertThat(resp.honorarios()).hasSize(3);
        assertThat(resp.resumo().totalProcessos()).isEqualTo(1);
        assertThat(resp.resumo().emAberto()).isEqualByComparingTo("130.00");
    }

    @Test
    @DisplayName("porToken degrada com graca: se um servico falha, retorna vazio no lugar sem quebrar o resto")
    void porTokenDegradaComGraca() {
        when(repo.findByPortalToken(TOKEN)).thenReturn(Optional.of(clienteComToken()));
        when(processoClient.processosDoCliente(anyString(), anyString()))
                .thenThrow(new RuntimeException("processo-service fora do ar"));
        when(financeiroClient.honorariosDoCliente("c1", ORG))
                .thenReturn(List.of(honorario("PENDENTE", "80.00")));

        PortalResponse resp = service.porToken(TOKEN);

        assertThat(resp.processos()).isEmpty();
        assertThat(resp.honorarios()).hasSize(1);
        assertThat(resp.resumo().emAberto()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("porToken lanca 404 quando o token e invalido")
    void porTokenInvalido() {
        when(repo.findByPortalToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.porToken(TOKEN))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
