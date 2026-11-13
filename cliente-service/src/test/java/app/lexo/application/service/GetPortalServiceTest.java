package app.lexo.application.service;

import app.lexo.application.port.in.PortalResponse;
import app.lexo.application.port.out.LoadClientFeesPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.common.error.ResourceNotFoundException;
import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.domain.Client;
import app.lexo.domain.ClientData;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.PortalToken;
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

    private final List<PortalCase> cases =
            List.of(new PortalCase("P-1", "Civel", "ATIVO", Instant.now(), List.of(), List.of()));
    private final List<PortalFee> fees = List.of(
            new PortalFee("Consulta", new BigDecimal("100.00"), "PENDENTE", Instant.now()),
            new PortalFee("Audiencia", new BigDecimal("50.50"), "ATRASADO", Instant.now()),
            new PortalFee("Peticao", new BigDecimal("200.00"), "PAGO", Instant.now()));

    private String clientWithLink(long validityDays) {
        Client client = clients.save(Client.register(ORGANIZATION,
                new ClientData("Helena Prado", null, null, null, null)));
        String token = "a".repeat(64);
        client.generatePortalLink(PortalToken.hash(token), NOW, validityDays);
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
    @DisplayName("if the cases service fails, the portal fails instead of showing an empty section")
    void casesFailure() {
        String token = clientWithLink(90);

        assertThatThrownBy(() -> service(
                (e, c) -> {
                    throw new IllegalStateException("fora do ar");
                },
                (e, c) -> fees).get(token))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("if the fees service fails, the portal fails instead of reporting nothing outstanding")
    void feesFailure() {
        String token = clientWithLink(90);

        assertThatThrownBy(() -> service(
                (e, c) -> cases,
                (e, c) -> {
                    throw new IllegalStateException("fora do ar");
                }).get(token))
                .isInstanceOf(IllegalStateException.class);
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
