package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.out.LoadLatestAuditEntriesPort;
import app.lexo.application.port.out.SaveAuditEntryPort;
import app.lexo.domain.AuditEntry;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Audit entry persistence adapter (integration, Postgres)")
class AuditEntryPersistenceAdapterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SaveAuditEntryPort save;

    @Autowired
    private LoadLatestAuditEntriesPort load;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-12T10:15:30.123456Z");

    @Test
    @DisplayName("save returns the entry with an id and keeps the date of the event")
    void saveAssignsIdAndKeepsTheDate() {
        OrganizationId organization = newOrganization();

        AuditEntry saved = save.save(AuditEntry.register(organization, "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1",
                "Criado", OCCURRED_AT));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.organizationId()).isEqualTo(organization);
        assertThat(saved.userId()).isEqualTo("u-1");
        assertThat(saved.userName()).isEqualTo("Ana");
        assertThat(saved.entityType()).isEqualTo("CLIENTE");
        assertThat(saved.entityId()).isEqualTo("c-1");
        assertThat(saved.description()).isEqualTo("Criado");
    }

    @Test
    @DisplayName("an entry without user id or entity is stored and read back with those fields null")
    void optionalFieldsStayNull() {
        OrganizationId organization = newOrganization();

        save.save(AuditEntry.register(organization, null, "Sistema", "ALTEROU_PAPEL", null, null, "Papel alterado",
                OCCURRED_AT));

        AuditEntry read = load.latest(organization, 10).get(0);
        assertThat(read.userId()).isNull();
        assertThat(read.entityType()).isNull();
        assertThat(read.entityId()).isNull();
    }

    @Test
    @DisplayName("latest returns the newest first, only of the organization, up to the limit")
    void latest() {
        OrganizationId organization = newOrganization();
        OrganizationId other = newOrganization();
        for (int i = 1; i <= 5; i++) {
            save.save(AuditEntry.register(organization, "u", "Ana", "ACAO", null, null, "e" + i,
                    OCCURRED_AT.plusSeconds(i)));
        }
        save.save(AuditEntry.register(other, "u", "Ana", "ACAO", null, null, "de outro", OCCURRED_AT));

        assertThat(load.latest(organization, 3)).extracting(AuditEntry::description)
                .containsExactly("e5", "e4", "e3");
        assertThat(load.latest(other, 10)).extracting(AuditEntry::description).containsExactly("de outro");
        assertThat(load.latest(newOrganization(), 10)).isEmpty();
    }
}
