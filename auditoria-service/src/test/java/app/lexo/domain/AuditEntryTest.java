package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditEntry")
class AuditEntryTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-12T10:15:30Z");

    @Test
    @DisplayName("a new entry has the date of the event and no id until it is stored")
    void newEntry() {
        AuditEntry entry = AuditEntry.register(ORGANIZATION, "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1",
                "Cliente criado", OCCURRED_AT);

        assertThat(entry.id()).isNull();
        assertThat(entry.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(entry.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(entry.action()).isEqualTo("CLIENTE_CRIADO");
    }

    @Test
    @DisplayName("user id and entity are optional")
    void optionalFields() {
        AuditEntry entry = AuditEntry.register(ORGANIZATION, null, "Sistema", "ALTEROU_PAPEL", null, null, "Papel alterado",
                OCCURRED_AT);

        assertThat(entry.userId()).isNull();
        assertThat(entry.entityType()).isNull();
        assertThat(entry.entityId()).isNull();
    }

    @Test
    @DisplayName("the organization, the user name, the action, the description and the date are required")
    void requiredFields() {
        assertThatThrownBy(() -> AuditEntry.register(null, "u", "Ana", "A", null, null, "d", OCCURRED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", null, "A", null, null, "d", OCCURRED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", "Ana", null, null, null, "d", OCCURRED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", "Ana", "A", null, null, null, OCCURRED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", "Ana", "A", null, null, "d", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ids cannot be null or blank")
    void ids() {
        assertThatThrownBy(() -> new OrganizationId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrganizationId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntryId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntryId("")).isInstanceOf(IllegalArgumentException.class);
    }
}
