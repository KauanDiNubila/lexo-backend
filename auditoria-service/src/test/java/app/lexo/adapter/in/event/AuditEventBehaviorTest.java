package app.lexo.adapter.in.event;

import app.lexo.IntegrationTestBase;
import app.lexo.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Audit event consumer: behavior pinned before the hexagonal migration")
class AuditEventBehaviorTest extends IntegrationTestBase {

    private DomainEvent event(String orgId, String userName) {
        return new DomainEvent("CLIENTE_CRIADO", orgId, "CLIENTE", "c-42", "Cliente criado: Maria Souza",
                "u-7", userName, Instant.now().minusSeconds(86400));
    }

    @Test
    @DisplayName("listens to the lexo.eventos topic in the lexo-auditoria group")
    void topicAndGroup() {
        Listener listener = kafkaListener();

        assertThat(listener.annotation().topics()).containsExactly("lexo.eventos");
        assertThat(listener.annotation().groupId()).isEqualTo("lexo-auditoria");
    }

    @Test
    @DisplayName("an event becomes one entry with the event type as the action")
    void recordsTheEvent() throws Exception {
        String org = UUID.randomUUID().toString();

        deliver(event(org, "Ana Advogada"));

        assertThat(entriesOf(org)).hasSize(1);
        Map<String, Object> row = entriesOf(org).get(0);
        assertThat(row.get("action")).isEqualTo("CLIENTE_CRIADO");
        assertThat(row.get("user_id")).isEqualTo("u-7");
        assertThat(row.get("user_name")).isEqualTo("Ana Advogada");
        assertThat(row.get("entity_type")).isEqualTo("CLIENTE");
        assertThat(row.get("entity_id")).isEqualTo("c-42");
        assertThat(row.get("description")).isEqualTo("Cliente criado: Maria Souza");
        assertThat(row.get("id")).isNotNull();
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    @DisplayName("the entry is dated when it is recorded, not when the event happened")
    void datedWhenRecorded() throws Exception {
        String org = UUID.randomUUID().toString();
        Instant before = Instant.now();

        deliver(event(org, "Ana Advogada"));

        Instant createdAt = ((Timestamp) entriesOf(org).get(0).get("created_at")).toInstant();
        assertThat(createdAt).isAfterOrEqualTo(before.minusSeconds(1)).isBeforeOrEqualTo(Instant.now().plusSeconds(1));
    }

    @Test
    @DisplayName("an event without user id or entity is recorded with those fields empty")
    void optionalFields() throws Exception {
        String org = UUID.randomUUID().toString();

        deliver(new DomainEvent("ALTEROU_PAPEL", org, null, null, "Papel alterado", null, "Sistema", Instant.now()));

        Map<String, Object> row = entriesOf(org).get(0);
        assertThat(row.get("user_id")).isNull();
        assertThat(row.get("entity_type")).isNull();
        assertThat(row.get("entity_id")).isNull();
    }

    @Test
    @DisplayName("the same event delivered twice is recorded twice")
    void duplicateDelivery() throws Exception {
        String org = UUID.randomUUID().toString();
        DomainEvent event = event(org, "Ana Advogada");

        deliver(event);
        deliver(event);

        assertThat(entriesOf(org)).hasSize(2);
    }

    @Test
    @DisplayName("an event that cannot be stored is dropped without failing the consumer")
    void failureIsSwallowed() throws Exception {
        String org = UUID.randomUUID().toString();

        deliver(event(org, null));
        deliver(new DomainEvent("CLIENTE_CRIADO", null, "CLIENTE", "c-1", "sem escritorio", "u-1", "Ana", Instant.now()));
        deliver(new DomainEvent("CLIENTE_CRIADO", org, "CLIENTE", "c-1", null, "u-1", "Ana", Instant.now()));

        assertThat(entriesOf(org)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where description = 'sem escritorio'",
                Integer.class)).isZero();
    }

    @Test
    @DisplayName("the audit entry is visible to the organization's admin right after the event")
    void visibleThroughTheApi() throws Exception {
        Identity admin = Identity.create();

        deliver(event(admin.orgId(), "Ana Advogada"));

        getAs(admin, "/api/auditoria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CLIENTE_CRIADO"));
    }
}
