package app.lexo.application.service;

import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.out.LoadClientFeesPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.common.error.ResourceNotFoundException;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import app.lexo.domain.Client;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static app.lexo.application.service.InMemoryClients.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetPortalService")
class GetPortalServiceTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    private final InMemoryClients clients = new InMemoryClients();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final List<ProcessoPortal> cases =
            List.of(new ProcessoPortal("P-1", "Civel", "ATIVO", Instant.now(), List.of(), List.of()));
    private final List<HonorarioPortal> fees = List.of(
            new HonorarioPortal("Consulta", new BigDecimal("100.00"), "PENDENTE", Instant.now()),
            new HonorarioPortal("Audiencia", new BigDecimal("50.50"), "ATRASADO", Instant.now()),
            new HonorarioPortal("Peticao", new BigDecimal("200.00"), "PAGO", Instant.now()));

    private String clientWithLink(long validityDays) {
        Client client = clients.save(Client.register(ORGANIZATION,
                new ClientData("Helena Prado", null, null, null, null)));
        String token = "a".repeat(64);
        client.generatePortalLink(token, NOW, validityDays);
        clients.save(client);
        return token;
    }

    private GetPortalService service(LoadClientCasesPort casesPort,
                                           LoadClientFeesPort feesPort) {
        return new GetPortalService(clients, casesPort, feesPort, clock);
    }

    @Test
    @DisplayName("aggregates cases and fees and sums only what is outstanding")
    void aggregates() {
        String token = clientWithLink(90);

        PortalResponse response = service((e, c) -> cases, (e, c) -> fees).get(token);

        assertThat(response.client()).isEqualTo("Helena Prado");
        assertThat(response.cases()).hasSize(1);
        assertThat(response.fees()).hasSize(3);
        assertThat(response.summary().totalCases()).isEqualTo(1);
        assertThat(response.summary().outstanding()).isEqualByComparingTo("150.50");
    }

    @Test
    @DisplayName("if a service fails, its section comes back empty and the rest continues")
    void degrades() {
        String token = clientWithLink(90);

        PortalResponse response = service(
                (e, c) -> {
                    throw new IllegalStateException("fora do ar");
                },
                (e, c) -> fees).get(token);

        assertThat(response.cases()).isEmpty();
        assertThat(response.fees()).hasSize(3);
        assertThat(response.summary().totalCases()).isZero();
    }

    @Test
    @DisplayName("an unknown token returns not found")
    void invalidToken() {
        clientWithLink(90);

        assertThatThrownBy(() -> service((e, c) -> cases, (e, c) -> fees).get("outro"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Link inválido ou expirado");
    }

    @Test
    @DisplayName("an expired link returns not found")
    void expired() {
        String token = clientWithLink(-1);

        assertThatThrownBy(() -> service((e, c) -> cases, (e, c) -> fees).get(token))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Link inválido ou expirado");
    }
}
