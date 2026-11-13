package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditEntry")
class AuditEntryTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    @Test
    @DisplayName("a new entry has no id and no date until it is stored")
    void newEntry() {
        AuditEntry entry = AuditEntry.register(ORGANIZATION, "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1", "Cliente criado");

        assertThat(entry.id()).isNull();
        assertThat(entry.createdAt()).isNull();
        assertThat(entry.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(entry.action()).isEqualTo("CLIENTE_CRIADO");
    }

    @Test
    @DisplayName("user id and entity are optional")
    void optionalFields() {
        AuditEntry entry = AuditEntry.register(ORGANIZATION, null, "Sistema", "ALTEROU_PAPEL", null, null, "Papel alterado");

        assertThat(entry.userId()).isNull();
        assertThat(entry.entityType()).isNull();
        assertThat(entry.entityId()).isNull();
    }

    @Test
    @DisplayName("the organization, the user name, the action and the description are required")
    void requiredFields() {
        assertThatThrownBy(() -> AuditEntry.register(null, "u", "Ana", "A", null, null, "d"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", null, "A", null, null, "d"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", "Ana", null, null, null, "d"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditEntry.register(ORGANIZATION, "u", "Ana", "A", null, null, null))
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
