package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import app.lexo.common.evento.EventoDominio;
import app.lexo.common.referencia.ExisteResponse;
import app.lexo.evento.ConsumidorEventos;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Invoices API: behavior pinned before the hexagonal migration")
class InvoiceApiBehaviorTest extends IntegrationTestBase {

    @Autowired
    private ConsumidorEventos consumer;

    private final Instant in30Days = Instant.now().plus(Duration.ofDays(30));

    private JsonNode invoiceOf(Identity id, String invoiceId) throws Exception {
        for (JsonNode invoice : bodyOf(getAs(id, "/api/financeiro?size=100")).get("content")) {
            if (invoiceId.equals(invoice.get("id").asText())) {
                return invoice;
            }
        }
        throw new AssertionError("invoice " + invoiceId + " not found");
    }

    private EventoDominio event(String type, Identity id, String entityId) {
        return EventoDominio.de(type, id.orgId(), "CLIENTE", entityId, "description", id.userId(), id.name());
    }

    @Test
    @DisplayName("creating returns the full invoice and stamps paidAt only when PAGO")
    void create() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();

        postAs(id, "/api/financeiro", invoiceData(clientId, "Consulta", "150.00", "PENDENTE", in30Days))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.caseId").value(nullValue()))
                .andExpect(jsonPath("$.description").value("Consulta"))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.paidAt").value(nullValue()));

        postAs(id, "/api/financeiro", invoiceData(clientId, "Audiencia", "80.00", "PAGO", in30Days))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    @DisplayName("a blank caseId is stored as null and the case service is not asked")
    void blankCaseId() throws Exception {
        Identity id = Identity.create();
        Map<String, Object> body = invoiceData(newId(), "Consulta", "10.00", "PENDENTE", in30Days);
        body.put("caseId", "  ");
        when(caseService.processoExiste(anyString(), anyString()))
                .thenThrow(new IllegalStateException("must not be called"));

        postAs(id, "/api/financeiro", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(nullValue()));
    }

    @Test
    @DisplayName("an unknown client or case returns 404 and nothing is saved")
    void unknownReferences() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        when(clientService.clienteExiste(eq(clientId), anyString()))
                .thenReturn(new ExisteResponse(false));

        postAs(id, "/api/financeiro", invoiceData(clientId, "Consulta", "10.00", "PENDENTE", in30Days))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado"));

        String caseId = newId();
        when(caseService.processoExiste(eq(caseId), anyString()))
                .thenReturn(new ExisteResponse(false));
        Map<String, Object> body = invoiceData(newId(), "Consulta", "10.00", "PENDENTE", in30Days);
        body.put("caseId", caseId);
        postAs(id, "/api/financeiro", body)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Processo não encontrado"));

        getAs(id, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("when the client or case service is down the answer is 503")
    void servicesDown() throws Exception {
        Identity id = Identity.create();
        when(clientService.clienteExiste(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        postAs(id, "/api/financeiro", invoiceData(newId(), "Consulta", "10.00", "PENDENTE", in30Days))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("Serviço de clientes temporariamente indisponível. Tente novamente em instantes."));

        doReturn(new ExisteResponse(true)).when(clientService).clienteExiste(anyString(), anyString());
        when(caseService.processoExiste(anyString(), anyString())).thenThrow(new RuntimeException("down"));
        Map<String, Object> body = invoiceData(newId(), "Consulta", "10.00", "PENDENTE", in30Days);
        body.put("caseId", newId());
        postAs(id, "/api/financeiro", body)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("Serviço de processos temporariamente indisponível. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("validation: missing client, description, due date or status returns 400")
    void validation() throws Exception {
        Identity id = Identity.create();
        Map<String, Object> empty = new HashMap<>();
        empty.put("clientId", "");
        empty.put("description", "");
        empty.put("amount", 10);
        empty.put("status", "PENDENTE");

        postAs(id, "/api/financeiro", empty)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error", containsString("Selecione um cliente")))
                .andExpect(jsonPath("$.error", containsString("Descrição é obrigatória")))
                .andExpect(jsonPath("$.error", containsString("Vencimento é obrigatório")));

        Map<String, Object> unknownStatus = invoiceData(newId(), "x", "10.00", "INEXISTENTE", in30Days);
        postAs(id, "/api/financeiro", unknownStatus)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"));
    }

    @Test
    @DisplayName("list orders by due date, paginates and shows only the organization")
    void list() throws Exception {
        Identity id = Identity.create();
        Instant base = Instant.parse("2027-01-10T12:00:00Z");
        createInvoice(id, newId(), "third", "3.00", "PENDENTE", base.plus(Duration.ofDays(3)));
        createInvoice(id, newId(), "first", "1.00", "PENDENTE", base.plus(Duration.ofDays(1)));
        createInvoice(id, newId(), "second", "2.00", "PENDENTE", base.plus(Duration.ofDays(2)));
        createInvoice(Identity.create(), newId(), "other organization", "9.00", "PENDENTE", base);

        getAs(id, "/api/financeiro")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].description").value("first"))
                .andExpect(jsonPath("$.content[2].description").value("third"));
        getAs(id, "/api/financeiro?size=2&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        getAs(id, "/api/financeiro?sort=amount,desc")
                .andExpect(jsonPath("$.content[0].description").value("third"));
    }

    @Test
    @DisplayName("updating changes the fields; another organization gets 404")
    void update() throws Exception {
        Identity id = Identity.create();
        String invoiceId = createInvoice(id, newId(), "old", "10.00", "PENDENTE", in30Days);
        String newClientId = newId();

        putAs(id, "/api/financeiro/" + invoiceId, invoiceData(newClientId, "new", "25.50", "ATRASADO", in30Days))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId))
                .andExpect(jsonPath("$.clientId").value(newClientId))
                .andExpect(jsonPath("$.description").value("new"))
                .andExpect(jsonPath("$.amount").value(25.5))
                .andExpect(jsonPath("$.status").value("ATRASADO"));

        putAs(Identity.create(), "/api/financeiro/" + invoiceId, invoiceData(newId(), "x", "1.00", "PENDENTE", in30Days))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Honorário não encontrado"))
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("today: editing an invoice that is already PAGO stamps paidAt again")
    void updateRestampsPaidAt() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        String invoiceId = createInvoice(id, clientId, "paid", "10.00", "PAGO", in30Days);
        Instant first = Instant.parse(invoiceOf(id, invoiceId).get("paidAt").asText());
        Thread.sleep(50);

        putAs(id, "/api/financeiro/" + invoiceId, invoiceData(clientId, "only the description changed", "10.00", "PAGO", in30Days))
                .andExpect(status().isOk());

        Instant second = Instant.parse(invoiceOf(id, invoiceId).get("paidAt").asText());
        assertThat(second).isAfter(first);
    }

    @Test
    @DisplayName("updating from PAGO to another status clears paidAt")
    void updateClearsPaidAt() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        String invoiceId = createInvoice(id, clientId, "paid", "10.00", "PAGO", in30Days);

        putAs(id, "/api/financeiro/" + invoiceId, invoiceData(clientId, "paid", "10.00", "PENDENTE", in30Days))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidAt").value(nullValue()));
    }

    @Test
    @DisplayName("changing the status to PAGO stamps paidAt; today, leaving PAGO keeps it")
    void changeStatus() throws Exception {
        Identity id = Identity.create();
        String invoiceId = createInvoice(id, newId(), "x", "10.00", "PENDENTE", in30Days);

        patchAs(id, "/api/financeiro/" + invoiceId + "/status", Map.of("status", "PAGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.paidAt").exists());

        patchAs(id, "/api/financeiro/" + invoiceId + "/status", Map.of("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    @DisplayName("changing the status does not ask the client or case service, and other organizations get 404")
    void changeStatusScope() throws Exception {
        Identity id = Identity.create();
        String invoiceId = createInvoice(id, newId(), "x", "10.00", "PENDENTE", in30Days);
        when(clientService.clienteExiste(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        patchAs(id, "/api/financeiro/" + invoiceId + "/status", Map.of("status", "CANCELADO")).andExpect(status().isOk());
        patchAs(Identity.create(), "/api/financeiro/" + invoiceId + "/status", Map.of("status", "PAGO"))
                .andExpect(status().isNotFound());
        patchAs(id, "/api/financeiro/" + invoiceId + "/status", Map.of("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("delete removes the invoice; another organization gets 404; a second delete gets 404")
    void delete() throws Exception {
        Identity id = Identity.create();
        String invoiceId = createInvoice(id, newId(), "x", "10.00", "PENDENTE", in30Days);

        deleteAs(Identity.create(), "/api/financeiro/" + invoiceId).andExpect(status().isNotFound());
        deleteAs(id, "/api/financeiro/" + invoiceId).andExpect(status().isNoContent());
        deleteAs(id, "/api/financeiro/" + invoiceId).andExpect(status().isNotFound());
        getAs(id, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("summary: outstanding is PENDENTE plus ATRASADO, received is PAGO, CANCELADO counts for neither")
    void summary() throws Exception {
        Identity id = Identity.create();
        createInvoice(id, newId(), "a", "100.00", "PENDENTE", in30Days);
        createInvoice(id, newId(), "b", "50.50", "ATRASADO", in30Days);
        createInvoice(id, newId(), "c", "200.00", "PAGO", in30Days);
        createInvoice(id, newId(), "d", "999.00", "CANCELADO", in30Days);

        getAs(id, "/api/financeiro/resumo")
                .andExpect(jsonPath("$.emAberto").value(150.5))
                .andExpect(jsonPath("$.recebido").value(200.0));
        getAs(Identity.create(), "/api/financeiro/resumo")
                .andExpect(jsonPath("$.emAberto").value(0))
                .andExpect(jsonPath("$.recebido").value(0));
    }

    @Test
    @DisplayName("report sums by status inside the period, both ends included, ignoring CANCELADO")
    void report() throws Exception {
        Identity id = Identity.create();
        Instant start = Instant.parse("2027-03-10T12:00:00Z");
        Instant end = Instant.parse("2027-03-20T12:00:00Z");
        createInvoice(id, newId(), "at start", "100.00", "PAGO", start);
        createInvoice(id, newId(), "inside", "40.50", "PENDENTE", start.plus(Duration.ofDays(2)));
        createInvoice(id, newId(), "at end", "10.25", "ATRASADO", end);
        createInvoice(id, newId(), "cancelled", "999.00", "CANCELADO", start.plus(Duration.ofDays(3)));
        createInvoice(id, newId(), "before", "500.00", "PAGO", start.minusSeconds(1));
        createInvoice(id, newId(), "after", "500.00", "PENDENTE", end.plusSeconds(1));

        getAs(id, "/api/financeiro/relatorio?start=" + start + "&end=" + end)
                .andExpect(jsonPath("$.totalPago").value(100.0))
                .andExpect(jsonPath("$.totalPendente").value(40.5))
                .andExpect(jsonPath("$.totalAtrasado").value(10.25))
                .andExpect(jsonPath("$.start").value(start.toString()))
                .andExpect(jsonPath("$.end").value(end.toString()));
    }

    @Test
    @DisplayName("an empty period returns zeros, not nulls")
    void emptyReport() throws Exception {
        Identity id = Identity.create();

        getAs(id, "/api/financeiro/relatorio?start=2030-01-01T00:00:00Z&end=2030-02-01T00:00:00Z")
                .andExpect(jsonPath("$.totalPago").value(0))
                .andExpect(jsonPath("$.totalPendente").value(0))
                .andExpect(jsonPath("$.totalAtrasado").value(0));
    }

    @Test
    @DisplayName("CLIENTE_EXCLUIDO deletes the client's invoices only in that organization; other event types are ignored")
    void clientDeletedEvent() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String deletedClient = newId();
        String otherClient = newId();
        createInvoice(a, deletedClient, "gone 1", "1.00", "PENDENTE", in30Days);
        createInvoice(a, deletedClient, "gone 2", "2.00", "PAGO", in30Days);
        createInvoice(a, otherClient, "stays", "3.00", "PENDENTE", in30Days);
        createInvoice(b, deletedClient, "other organization stays", "4.00", "PENDENTE", in30Days);

        consumer.consumir(event("CLIENTE_CRIADO", a, deletedClient));
        getAs(a, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(3));

        consumer.consumir(event("CLIENTE_EXCLUIDO", a, deletedClient));

        getAs(a, "/api/financeiro")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("stays"));
        getAs(b, "/api/financeiro").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PROCESSO_EXCLUIDO detaches the case from the invoices without deleting them")
    void caseDeletedEvent() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        String caseId = newId();
        Map<String, Object> body = invoiceData(newId(), "linked", "10.00", "PENDENTE", in30Days);
        body.put("caseId", caseId);
        String invoiceId = bodyOf(postAs(a, "/api/financeiro", body).andExpect(status().isCreated())).get("id").asText();
        String otherOrganizationInvoice = bodyOf(postAs(b, "/api/financeiro", body).andExpect(status().isCreated()))
                .get("id").asText();

        consumer.consumir(event("PROCESSO_EXCLUIDO", a, caseId));

        assertThat(invoiceOf(a, invoiceId).get("caseId").isNull()).isTrue();
        assertThat(invoiceOf(b, otherOrganizationInvoice).get("caseId").asText()).isEqualTo(caseId);
    }

    @Test
    @DisplayName("the portal endpoint lists the client's invoices by due date, only in that organization")
    void portalEndpoint() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        Instant base = Instant.parse("2027-05-01T12:00:00Z");
        createInvoice(id, clientId, "later", "20.00", "ATRASADO", base.plus(Duration.ofDays(2)));
        createInvoice(id, clientId, "sooner", "10.00", "PENDENTE", base.plus(Duration.ofDays(1)));
        createInvoice(id, newId(), "other client", "5.00", "PENDENTE", base);

        mvc.perform(get("/internal/portal/honorarios").param("clientId", clientId).param("orgId", id.orgId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("sooner"))
                .andExpect(jsonPath("$[0].amount").value(10.0))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].dueDate").exists())
                .andExpect(jsonPath("$[1].description").value("later"));
        mvc.perform(get("/internal/portal/honorarios").param("clientId", clientId).param("orgId", newId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
