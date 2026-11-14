package app.lexo.adapter.in.event;

import app.lexo.IntegrationTestBase;
import app.lexo.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Entity deleted listener (integration)")
class EntityDeletedListenerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EntityDeletedListener listener;

    private final Instant in30Days = Instant.now().plus(Duration.ofDays(30));

    private DomainEvent event(String type, Identity id, String entityId) {
        return DomainEvent.of(type, id.orgId(), "CLIENTE", entityId, "description", id.userId(), id.name());
    }

    @Test
    @DisplayName("CLIENTE_EXCLUIDO deletes the client's invoices only in that organization; other event types are ignored")
    void clientDeleted() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String deletedClient = newId();
        String otherClient = newId();
        createInvoice(a, deletedClient, "gone 1", "1.00", "PENDENTE", in30Days);
        createInvoice(a, deletedClient, "gone 2", "2.00", "PAGO", in30Days);
        createInvoice(a, otherClient, "stays", "3.00", "PENDENTE", in30Days);
        createInvoice(b, deletedClient, "other organization stays", "4.00", "PENDENTE", in30Days);

        listener.onEvent(event("CLIENTE_CRIADO", a, deletedClient));
        getAs(a, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(3));

        listener.onEvent(event("CLIENTE_EXCLUIDO", a, deletedClient));

        getAs(a, "/api/financeiro")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("stays"));
        getAs(b, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PROCESSO_EXCLUIDO detaches the case from the invoices without deleting them")
    void caseDeleted() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String caseId = newId();
        Map<String, Object> body = invoiceData(newId(), "linked", "10.00", "PENDENTE", in30Days);
        body.put("caseId", caseId);
        String invoiceId = bodyOf(postAs(a, "/api/financeiro", body).andExpect(status().isCreated())).get("id").asText();
        String otherInvoiceId = bodyOf(postAs(b, "/api/financeiro", body).andExpect(status().isCreated())).get("id").asText();

        listener.onEvent(event("PROCESSO_EXCLUIDO", a, caseId));

        assertThat(caseIdOf(a, invoiceId)).isNull();
        assertThat(caseIdOf(b, otherInvoiceId)).isEqualTo(caseId);
    }

    private String caseIdOf(Identity id, String invoiceId) throws Exception {
        for (var invoice : bodyOf(getAs(id, "/api/financeiro?size=100")).get("content")) {
            if (invoiceId.equals(invoice.get("id").asText())) {
                return invoice.get("caseId").isNull() ? null : invoice.get("caseId").asText();
            }
        }
        throw new AssertionError("invoice " + invoiceId + " not found");
    }
}
