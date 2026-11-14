package app.lexo.application.service;

import app.lexo.application.port.out.DeadlineReminder;
import app.lexo.application.port.out.UserContact;
import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deadline reminders (in-memory ports)")
class SendDeadlineRemindersServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");

    private final InMemoryCases cases = new InMemoryCases();
    private final InMemoryDeadlines deadlines = new InMemoryDeadlines();
    private final Map<String, UserContact> contacts = new HashMap<>();
    private final List<DeadlineReminder> sent = new ArrayList<>();
    private final List<String> lookups = new ArrayList<>();
    private boolean windowFree = true;
    private boolean queueUp = true;
    private final SendDeadlineRemindersService service = new SendDeadlineRemindersService(
            (key, ttl) -> windowFree, deadlines, cases,
            (organization, user) -> {
                lookups.add(user.value());
                return Optional.ofNullable(contacts.get(user.value()));
            },
            reminder -> queueUp && sent.add(reminder), deadlines, Clock.fixed(NOW, ZoneOffset.UTC));

    private CaseId openCase(String responsible) {
        return cases.save(Case.open(ORGANIZATION, new CaseData(new ClientId("client-1"), "N-" + sent.size(), null,
                CaseStatus.ACTIVE, null, UserId.optional(responsible)))).id();
    }

    private Deadline deadlineAt(CaseId caseId, Duration fromNow) {
        return deadlines.save(Deadline.schedule(ORGANIZATION,
                new DeadlineData(caseId, "Respond", DeadlineType.DEADLINE, NOW.plus(fromNow), null)));
    }

    @Test
    @DisplayName("reminds the responsible of pending deadlines in the next 3 days, once")
    void remindsOnce() {
        contacts.put("user-1", new UserContact("Ana", "ana@firm.com"));
        CaseId caseId = openCase("user-1");
        deadlineAt(caseId, Duration.ofDays(1));
        deadlineAt(caseId, Duration.ofDays(10));
        deadlineAt(caseId, Duration.ofDays(-1));

        assertThat(service.sendReminders()).isEqualTo(1);

        assertThat(sent).singleElement().satisfies(reminder -> {
            assertThat(reminder.recipientEmail()).isEqualTo("ana@firm.com");
            assertThat(reminder.deadlineTitle()).isEqualTo("Respond");
        });
        assertThat(deadlines.all().stream().filter(deadline -> deadline.notifiedAt() != null)).hasSize(1);
        assertThat(service.sendReminders()).isZero();
        assertThat(sent).hasSize(1);
    }

    @Test
    @DisplayName("skips a case without responsible and a responsible without e-mail, leaving them unmarked")
    void skipsWithoutRecipient() {
        contacts.put("no-email", new UserContact("Bia", ""));
        deadlineAt(openCase(null), Duration.ofDays(1));
        deadlineAt(openCase("no-email"), Duration.ofDays(1));
        deadlineAt(openCase("unknown"), Duration.ofDays(1));

        assertThat(service.sendReminders()).isZero();

        assertThat(sent).isEmpty();
        assertThat(deadlines.all()).allMatch(deadline -> deadline.notifiedAt() == null);
    }

    @Test
    @DisplayName("with the window already claimed by another run, does nothing at all")
    void windowAlreadyClaimed() {
        contacts.put("user-1", new UserContact("Ana", "ana@firm.com"));
        deadlineAt(openCase("user-1"), Duration.ofDays(1));
        windowFree = false;

        assertThat(service.sendReminders()).isZero();

        assertThat(lookups).isEmpty();
        assertThat(sent).isEmpty();
        assertThat(deadlines.all().get(0).notifiedAt()).isNull();
    }

    @Test
    @DisplayName("looks each responsible up only once per run, even with several deadlines")
    void looksUpEachResponsibleOnce() {
        contacts.put("user-1", new UserContact("Ana", "ana@firm.com"));
        CaseId caseId = openCase("user-1");
        deadlineAt(caseId, Duration.ofDays(1));
        deadlineAt(caseId, Duration.ofDays(2));
        deadlineAt(openCase("user-1"), Duration.ofDays(1));

        assertThat(service.sendReminders()).isEqualTo(3);

        assertThat(lookups).containsExactly("user-1");
        assertThat(sent).hasSize(3);
    }

    @Test
    @DisplayName("a reminder that could not be queued is not counted and leaves the deadline unmarked, so the next run retries")
    void unqueuedReminderIsRetried() {
        contacts.put("user-1", new UserContact("Ana", "ana@firm.com"));
        deadlineAt(openCase("user-1"), Duration.ofDays(1));
        queueUp = false;

        assertThat(service.sendReminders()).isZero();
        assertThat(deadlines.all().get(0).notifiedAt()).isNull();

        queueUp = true;
        assertThat(service.sendReminders()).isEqualTo(1);
        assertThat(deadlines.all().get(0).notifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("with nothing in the window, returns zero without looking anyone up")
    void nothingToRemind() {
        deadlineAt(openCase("user-1"), Duration.ofDays(20));

        assertThat(service.sendReminders()).isZero();

        assertThat(lookups).isEmpty();
    }
}
