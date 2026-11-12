package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.out.DeleteDeadlinePort;
import app.lexo.application.port.out.DeleteDeadlinesOfCasePort;
import app.lexo.application.port.out.ListDeadlinesPort;
import app.lexo.application.port.out.LoadDeadlinePort;
import app.lexo.application.port.out.LoadDeadlinesOfCasesPort;
import app.lexo.application.port.out.LoadDeadlinesToRemindPort;
import app.lexo.application.port.out.PendingDeadlinesPort;
import app.lexo.application.port.out.SaveDeadlinePort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.common.web.SortOrder;
import app.lexo.domain.CaseId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineId;
import app.lexo.domain.DeadlineStatus;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deadline persistence adapter (integration, Postgres)")
class DeadlinePersistenceAdapterIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Autowired
    private SaveDeadlinePort save;
    @Autowired
    private LoadDeadlinePort load;
    @Autowired
    private DeleteDeadlinePort delete;
    @Autowired
    private ListDeadlinesPort list;
    @Autowired
    private PendingDeadlinesPort pending;
    @Autowired
    private DeleteDeadlinesOfCasePort deleteOfCase;
    @Autowired
    private LoadDeadlinesToRemindPort toRemind;
    @Autowired
    private LoadDeadlinesOfCasesPort ofCases;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private Deadline saveDeadline(OrganizationId organization, CaseId caseId, Duration fromNow) {
        return save.save(Deadline.schedule(organization,
                new DeadlineData(caseId, "Respond", DeadlineType.DEADLINE, NOW.plus(fromNow), "text")));
    }

    @Test
    @DisplayName("round trips a deadline with its type, status and notification mark, stored with the old codes")
    void roundTrip() {
        OrganizationId organization = newOrganization();
        Deadline saved = saveDeadline(organization, new CaseId("case-1"), Duration.ofDays(2));
        saved.complete();
        saved.markNotified(NOW);

        save.save(saved);

        Deadline read = load.byId(organization, saved.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(DeadlineStatus.COMPLETED);
        assertThat(read.notifiedAt()).isEqualTo(
                jdbc.queryForObject("select notified_at from deadlines where id = ?", java.sql.Timestamp.class,
                        saved.id().value()).toInstant());
        assertThat(jdbc.queryForObject("select type from deadlines where id = ?", String.class, saved.id().value()))
                .isEqualTo("PRAZO");
        assertThat(jdbc.queryForObject("select status from deadlines where id = ?", String.class,
                saved.id().value())).isEqualTo("CONCLUIDO");
        assertThat(load.byId(newOrganization(), saved.id())).isEmpty();
    }

    @Test
    @DisplayName("lists by organization or by case, nearest first")
    void listing() {
        OrganizationId organization = newOrganization();
        CaseId first = new CaseId("case-a");
        CaseId second = new CaseId("case-b");
        saveDeadline(organization, first, Duration.ofDays(30));
        saveDeadline(organization, first, Duration.ofDays(5));
        saveDeadline(organization, second, Duration.ofDays(9));
        saveDeadline(newOrganization(), first, Duration.ofDays(1));
        PageParams nearestFirst = new PageParams(0, 20, List.of(new SortOrder("date", true)));

        PageResult<Deadline> all = list.list(organization, null, nearestFirst);
        PageResult<Deadline> ofSecond = list.list(organization, second, nearestFirst);

        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.content()).extracting(deadline -> deadline.data().date()).isSorted();
        assertThat(ofSecond.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("counts pending deadlines in a window and before a limit, and returns the nearest ones")
    void pendingQueries() {
        OrganizationId organization = newOrganization();
        CaseId caseId = new CaseId("case-p");
        Deadline overdue = saveDeadline(organization, caseId, Duration.ofDays(-3));
        Deadline soon = saveDeadline(organization, caseId, Duration.ofDays(2));
        saveDeadline(organization, caseId, Duration.ofDays(5));
        saveDeadline(organization, caseId, Duration.ofDays(20));
        Deadline done = saveDeadline(organization, caseId, Duration.ofDays(1));
        done.complete();
        save.save(done);

        assertThat(pending.countBetween(organization, NOW, NOW.plus(Duration.ofDays(7)))).isEqualTo(2);
        assertThat(pending.countBefore(organization, NOW)).isEqualTo(1);
        assertThat(pending.next(organization, 2)).extracting(Deadline::id).containsExactly(overdue.id(), soon.id());
    }

    @Test
    @Transactional
    @DisplayName("deleting by id and by case only reaches the organization's own deadlines")
    void deleteScope() {
        OrganizationId owner = newOrganization();
        CaseId caseId = new CaseId("case-d");
        Deadline first = saveDeadline(owner, caseId, Duration.ofDays(1));
        Deadline second = saveDeadline(owner, caseId, Duration.ofDays(2));

        delete.delete(newOrganization(), first.id());
        deleteOfCase.deleteOfCase(newOrganization(), caseId);
        assertThat(load.byId(owner, first.id())).isPresent();

        delete.delete(owner, first.id());
        assertThat(load.byId(owner, first.id())).isEmpty();
        deleteOfCase.deleteOfCase(owner, caseId);
        assertThat(load.byId(owner, second.id())).isEmpty();
    }

    @Test
    @DisplayName("finds the pending, not yet notified deadlines inside the reminder window")
    void remindWindow() {
        OrganizationId organization = newOrganization();
        CaseId caseId = new CaseId("case-r");
        Deadline inside = saveDeadline(organization, caseId, Duration.ofDays(1));
        Deadline outside = saveDeadline(organization, caseId, Duration.ofDays(10));
        Deadline notified = saveDeadline(organization, caseId, Duration.ofDays(2));
        notified.markNotified(NOW);
        save.save(notified);
        Deadline completed = saveDeadline(organization, caseId, Duration.ofDays(2));
        completed.complete();
        save.save(completed);

        List<DeadlineId> ids = toRemind.pendingNotNotifiedBetween(NOW, NOW.plus(Duration.ofDays(3))).stream()
                .map(Deadline::id).toList();

        assertThat(ids).contains(inside.id()).doesNotContain(outside.id(), notified.id(), completed.id());
    }

    @Test
    @DisplayName("loads the deadlines of several cases of one organization, nearest first")
    void ofSeveralCases() {
        OrganizationId organization = newOrganization();
        CaseId first = new CaseId("case-x");
        CaseId second = new CaseId("case-y");
        saveDeadline(organization, first, Duration.ofDays(9));
        saveDeadline(organization, second, Duration.ofDays(3));
        saveDeadline(newOrganization(), first, Duration.ofDays(1));

        List<Deadline> found = ofCases.ofCases(organization, List.of(first, second));

        assertThat(found).hasSize(2);
        assertThat(found).extracting(deadline -> deadline.data().date()).isSorted();
    }
}
