package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import app.lexo.common.email.RabbitConfig;
import app.lexo.common.email.TarefaEmail;
import app.lexo.common.user.InternalUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Deadline reminders (integration)")
class DeadlineReminderBehaviorTest extends IntegrationTestBase {

    private static final String CRON_URL = "/internal/cron/notify-deadlines";

    private ValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void windowIsFree() {
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    private int runReminders() throws Exception {
        return bodyOf(mvc.perform(post(CRON_URL).header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk()))
                .get("sent").asInt();
    }

    private String caseWithResponsible(Identity id, String responsibleId, String number) throws Exception {
        Map<String, Object> data = new HashMap<>(caseData(newId(), number));
        if (responsibleId != null) {
            data.put("responsavelId", responsibleId);
        }
        return idOf(postAs(id, "/api/processos", data).andExpect(status().isCreated()));
    }

    private void responsibleWithEmail(String responsibleId, String email) {
        when(userService.findUser(eq(responsibleId), anyString()))
                .thenReturn(new InternalUser(responsibleId, "Ana Lawyer", email));
    }

    private void verifyEmails(String address, int times) {
        verify(rabbit, times(times)).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                argThat((Object task) -> task instanceof TarefaEmail email && address.equals(email.destinatario())));
    }

    private boolean wasNotified(String deadlineId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select notified_at is not null from deadlines where id = ?", Boolean.class, deadlineId));
    }

    @Test
    @DisplayName("notifies the responsible of pending deadlines in the next 3 days, once")
    void notifiesOnce() throws Exception {
        Identity id = Identity.create();
        String responsibleId = newId();
        String address = UUID.randomUUID() + "@firm.com";
        responsibleWithEmail(responsibleId, address);
        String caseId = caseWithResponsible(id, responsibleId, "NT-001");
        String inside = createDeadline(id, caseId, "PRAZO", Duration.ofDays(1));
        String far = createDeadline(id, caseId, "PRAZO", Duration.ofDays(10));
        String past = createDeadline(id, caseId, "PRAZO", Duration.ofDays(-1));
        String completed = createDeadline(id, caseId, "PRAZO", Duration.ofDays(2));
        toggleStatus(id, completed, true).andExpect(status().isOk());

        assertThat(runReminders()).isEqualTo(1);

        verifyEmails(address, 1);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                argThat((Object task) -> task instanceof TarefaEmail email
                        && "LEMBRETE_PRAZO".equals(email.tipo()) && email.assunto().startsWith("Prazo próximo: ")));
        assertThat(wasNotified(inside)).isTrue();
        assertThat(wasNotified(far)).isFalse();
        assertThat(wasNotified(past)).isFalse();
        assertThat(wasNotified(completed)).isFalse();

        assertThat(runReminders()).isZero();

        verifyEmails(address, 1);
    }

    @Test
    @DisplayName("skips a deadline without responsible and a responsible without e-mail, without marking them")
    void skipsWithoutRecipient() throws Exception {
        Identity id = Identity.create();
        String withoutEmail = newId();
        responsibleWithEmail(withoutEmail, "");
        String caseWithoutResponsible = caseWithResponsible(id, null, "NT-002");
        String caseWithoutEmail = caseWithResponsible(id, withoutEmail, "NT-003");
        String deadlineA = createDeadline(id, caseWithoutResponsible, "PRAZO", Duration.ofDays(1));
        String deadlineB = createDeadline(id, caseWithoutEmail, "PRAZO", Duration.ofDays(1));

        assertThat(runReminders()).isZero();

        verifyEmails("", 0);
        assertThat(wasNotified(deadlineA)).isFalse();
        assertThat(wasNotified(deadlineB)).isFalse();
    }

    @Test
    @DisplayName("with the window already claimed by another run, does nothing")
    void windowAlreadyClaimed() throws Exception {
        Identity id = Identity.create();
        String responsibleId = newId();
        responsibleWithEmail(responsibleId, UUID.randomUUID() + "@firm.com");
        String caseId = caseWithResponsible(id, responsibleId, "NT-004");
        String deadlineId = createDeadline(id, caseId, "PRAZO", Duration.ofDays(1));
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThat(runReminders()).isZero();

        verifyNoInteractions(rabbit);
        verify(userService, never()).findUser(anyString(), anyString());
        assertThat(wasNotified(deadlineId)).isFalse();
    }

    @Test
    @DisplayName("when the queue is down the deadline stays pending and is reminded once the queue is back")
    void queueDownKeepsDeadlinePending() throws Exception {
        Identity id = Identity.create();
        String responsibleId = newId();
        responsibleWithEmail(responsibleId, UUID.randomUUID() + "@firm.com");
        String caseId = caseWithResponsible(id, responsibleId, "NT-005");
        String deadlineId = createDeadline(id, caseId, "PRAZO", Duration.ofDays(1));
        doThrow(new RuntimeException("queue down")).when(rabbit)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertThat(runReminders()).isZero();
        assertThat(wasNotified(deadlineId)).isFalse();

        reset(rabbit);
        assertThat(runReminders()).isEqualTo(1);
        assertThat(wasNotified(deadlineId)).isTrue();
    }

    @Test
    @DisplayName("moving a notified deadline to a new date re-arms its reminder")
    void rescheduledDeadlineIsRemindedAgain() throws Exception {
        Identity id = Identity.create();
        String responsibleId = newId();
        String address = UUID.randomUUID() + "@firm.com";
        responsibleWithEmail(responsibleId, address);
        String caseId = caseWithResponsible(id, responsibleId, "NT-006");
        String deadlineId = createDeadline(id, caseId, "PRAZO", Duration.ofDays(1));
        assertThat(runReminders()).isEqualTo(1);
        assertThat(runReminders()).isZero();

        putAs(id, "/api/agenda/" + deadlineId, Map.of("caseId", caseId, "title", "Respond", "type", "PRAZO",
                "date", Instant.now().plus(Duration.ofDays(2)).toString())).andExpect(status().isOk());

        assertThat(wasNotified(deadlineId)).isFalse();
        assertThat(runReminders()).isEqualTo(1);
        verifyEmails(address, 2);
    }

    @Test
    @DisplayName("the cron route requires the internal key")
    void cronRouteRequiresKey() throws Exception {
        mvc.perform(post(CRON_URL)).andExpect(status().isForbidden());
        mvc.perform(post(CRON_URL).header("X-Internal-Key", "chave-errada")).andExpect(status().isForbidden());
        mvc.perform(post(CRON_URL).header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.sent").value(0));
    }
}
