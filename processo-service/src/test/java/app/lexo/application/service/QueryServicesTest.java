package app.lexo.application.service;

import app.lexo.application.port.in.AgendaSummary;
import app.lexo.application.port.in.CaseOverview;
import app.lexo.application.port.in.CaseSummary;
import app.lexo.application.port.in.DeadlineResponse;
import app.lexo.application.port.out.PendingDeadlinesPort;
import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.MovementData;
import app.lexo.domain.MovementId;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Query services")
class QueryServicesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("the case summary totals the statuses and keeps the stored codes and their order")
    void caseSummary() {
        Map<CaseStatus, Long> counts = new LinkedHashMap<>();
        counts.put(CaseStatus.ACTIVE, 3L);
        counts.put(CaseStatus.SUSPENDED, 1L);
        counts.put(CaseStatus.ARCHIVED, 0L);
        counts.put(CaseStatus.CLOSED, 2L);
        CaseQueryService service = new CaseQueryService(null, null, null, organization -> counts, null);

        CaseSummary summary = service.summarize(ORGANIZATION);

        assertThat(summary.total()).isEqualTo(6);
        assertThat(summary.active()).isEqualTo(3);
        assertThat(summary.byStatus().keySet()).containsExactly("ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO");
    }

    @Test
    @DisplayName("getting an unknown case fails")
    void unknownCase() {
        CaseQueryService service = new CaseQueryService(null, new InMemoryCases(), null, null, null);

        assertThatThrownBy(() -> service.get(ORGANIZATION, new CaseId("ghost")))
                .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    @DisplayName("the agenda summary asks for the next 7 days, the overdue ones and the 5 nearest pending")
    void agendaSummary() {
        PendingDeadlinesPort pending = mock(PendingDeadlinesPort.class);
        Deadline next = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION, new DeadlineData(new CaseId("c-1"),
                "t", DeadlineType.DEADLINE, NOW.plus(Duration.ofDays(2)), null), DeadlineStatus.PENDING, null);
        when(pending.countBetween(ORGANIZATION, NOW, NOW.plus(Duration.ofDays(7)))).thenReturn(2L);
        when(pending.countBefore(ORGANIZATION, NOW)).thenReturn(1L);
        when(pending.next(ORGANIZATION, 5)).thenReturn(List.of(next));
        AgendaQueryService service = new AgendaQueryService(null, pending, CLOCK);

        AgendaSummary summary = service.summarize(ORGANIZATION);

        assertThat(summary.urgent()).isEqualTo(2);
        assertThat(summary.overdue()).isEqualTo(1);
        assertThat(summary.upcoming()).singleElement().extracting(DeadlineResponse::risk).isEqualTo("URGENTE");
    }

    @Test
    @DisplayName("the client overview groups each case's deadlines and movements, and is empty without cases")
    void clientOverview() {
        InMemoryCases cases = new InMemoryCases();
        ClientId client = new ClientId("client-1");
        CaseId first = cases.save(Case.open(ORGANIZATION,
                new CaseData(client, "A", null, CaseStatus.ACTIVE, null, null))).id();
        CaseId second = cases.save(Case.open(ORGANIZATION,
                new CaseData(client, "B", null, CaseStatus.ACTIVE, null, null))).id();
        Deadline deadline = Deadline.restore(new DeadlineId("d-1"), ORGANIZATION, new DeadlineData(first, "t",
                DeadlineType.HEARING, NOW.plus(Duration.ofDays(8)), null), DeadlineStatus.PENDING, null);
        CaseMovement movement = CaseMovement.restore(new MovementId("m-1"), ORGANIZATION, second,
                new MovementData("Summons", "text", NOW), NOW);
        ClientCasesQueryService service = new ClientCasesQueryService(cases,
                (organization, ids) -> List.of(deadline), (organization, ids) -> List.of(movement), CLOCK);

        List<CaseOverview> overview = service.list(ORGANIZATION, client);

        assertThat(overview).hasSize(2);
        assertThat(overview).filteredOn(item -> item.caseData().number().equals("A"))
                .singleElement().satisfies(item -> {
                    assertThat(item.deadlines()).hasSize(1);
                    assertThat(item.movements()).isEmpty();
                });
        assertThat(overview).filteredOn(item -> item.caseData().number().equals("B"))
                .singleElement().satisfies(item -> assertThat(item.movements()).hasSize(1));
        assertThat(service.list(ORGANIZATION, new ClientId("nobody"))).isEmpty();
    }
}
