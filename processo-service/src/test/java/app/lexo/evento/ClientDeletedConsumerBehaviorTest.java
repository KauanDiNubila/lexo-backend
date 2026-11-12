package app.lexo.evento;

import app.lexo.IntegrationTestBase;
import app.lexo.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Case service event consumer (integration)")
class ClientDeletedConsumerBehaviorTest extends IntegrationTestBase {

    @Autowired
    private ConsumidorEventos consumer;

    private DomainEvent event(String type, Identity id, String entityId) {
        return DomainEvent.of(type, id.orgId(), "CLIENTE", entityId, "description", id.userId(), id.name());
    }

    @Test
    @DisplayName("CLIENTE_EXCLUIDO deletes the client's cases with deadlines and movements, only in that organization")
    void clientDeletedCascades() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String deletedClient = newId();
        String otherClient = newId();
        String case1 = createCase(a, deletedClient, "EV-001");
        String case2 = createCase(a, deletedClient, "EV-002");
        String case3 = createCase(a, otherClient, "EV-003");
        String case4 = createCase(b, deletedClient, "EV-004");
        createDeadline(a, case1, "PRAZO", Duration.ofDays(10));
        postAs(a, "/api/processos/" + case1 + "/andamentos", Map.of("title", "Attachment", "date", Instant.now().toString()))
                .andExpect(status().isCreated());

        consumer.consumir(event("CLIENTE_EXCLUIDO", a, deletedClient));

        getAs(a, "/api/processos/" + case1).andExpect(status().isNotFound());
        getAs(a, "/api/processos/" + case2).andExpect(status().isNotFound());
        getAs(a, "/api/processos/" + case3).andExpect(status().isOk());
        getAs(a, "/api/agenda?caseId=" + case1).andExpect(jsonPath("$.totalElements").value(0));
        getAs(b, "/api/processos/" + case4).andExpect(status().isOk());
    }

    @Test
    @DisplayName("other event types are ignored")
    void otherTypesAreIgnored() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        String caseId = createCase(id, clientId, "EV-005");

        consumer.consumir(event("CLIENTE_CRIADO", id, clientId));
        consumer.consumir(event("PROCESSO_EXCLUIDO", id, caseId));

        getAs(id, "/api/processos/" + caseId).andExpect(status().isOk());
    }
}
