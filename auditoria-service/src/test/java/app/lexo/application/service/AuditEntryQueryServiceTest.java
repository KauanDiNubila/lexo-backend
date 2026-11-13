package app.lexo.application.service;

import app.lexo.application.port.in.AuditEntryResponse;
import app.lexo.domain.AuditEntry;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditEntryQueryService")
class AuditEntryQueryServiceTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("escritorio-1");
    private static final OrganizationId OTHER = new OrganizationId("escritorio-2");

    private final InMemoryAuditEntries entries = new InMemoryAuditEntries();
    private final AuditEntryQueryService service = new AuditEntryQueryService(entries);

    private int seconds;

    private void store(OrganizationId organization, String description) {
        entries.save(AuditEntry.register(organization, "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1", description,
                InMemoryAuditEntries.NOW.plusSeconds(seconds++)));
    }

    @Test
    @DisplayName("asks for the 200 latest entries")
    void asksForTwoHundred() {
        service.latest(ORGANIZATION);

        assertThat(entries.lastLimit).isEqualTo(200);
    }

    @Test
    @DisplayName("returns only the entries of the organization, newest first, with the public fields")
    void newestFirstAndScoped() {
        store(ORGANIZATION, "primeiro");
        store(OTHER, "de outro escritorio");
        store(ORGANIZATION, "segundo");

        List<AuditEntryResponse> result = service.latest(ORGANIZATION);

        assertThat(result).extracting(AuditEntryResponse::description).containsExactly("segundo", "primeiro");
        AuditEntryResponse first = result.get(0);
        assertThat(first.id()).isEqualTo("entry-3");
        assertThat(first.userName()).isEqualTo("Ana");
        assertThat(first.action()).isEqualTo("CLIENTE_CRIADO");
        assertThat(first.entityType()).isEqualTo("CLIENTE");
        assertThat(first.entityId()).isEqualTo("c-1");
        assertThat(first.occurredAt()).isEqualTo(InMemoryAuditEntries.NOW.plusSeconds(2));
    }

    @Test
    @DisplayName("is empty when the organization has no entries")
    void empty() {
        store(OTHER, "x");

        assertThat(service.latest(ORGANIZATION)).isEmpty();
    }
}
