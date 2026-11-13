package app.lexo.application.service;

import app.lexo.application.port.in.RegisterAuditEntryCommand;
import app.lexo.domain.AuditEntry;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegisterAuditEntryService")
class RegisterAuditEntryServiceTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");

    private final InMemoryAuditEntries entries = new InMemoryAuditEntries();
    private final RegisterAuditEntryService service = new RegisterAuditEntryService(entries);

    @Test
    @DisplayName("stores one entry with every field of the command")
    void storesTheEntry() {
        service.register(new RegisterAuditEntryCommand(ORGANIZATION, "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1",
                "Cliente criado: Maria"));

        assertThat(entries.all()).hasSize(1);
        AuditEntry entry = entries.all().get(0);
        assertThat(entry.organizationId()).isEqualTo(ORGANIZATION);
        assertThat(entry.userId()).isEqualTo("u-1");
        assertThat(entry.userName()).isEqualTo("Ana");
        assertThat(entry.action()).isEqualTo("CLIENTE_CRIADO");
        assertThat(entry.entityType()).isEqualTo("CLIENTE");
        assertThat(entry.entityId()).isEqualTo("c-1");
        assertThat(entry.description()).isEqualTo("Cliente criado: Maria");
    }

    @Test
    @DisplayName("lets a rule violation reach the caller and stores nothing")
    void propagatesInvalidEntries() {
        assertThatThrownBy(() -> service.register(
                new RegisterAuditEntryCommand(ORGANIZATION, "u-1", null, "CLIENTE_CRIADO", null, null, "d")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(entries.all()).isEmpty();
    }
}
