package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Invoices API security (integration)")
class InvoiceApiSecurityTest extends IntegrationTestBase {

    @Test
    @DisplayName("without identity, access is blocked")
    void noIdentity() throws Exception {
        mvc.perform(get("/api/financeiro")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("identity headers without the gateway signature are rejected")
    void unsignedHeaders() throws Exception {
        mvc.perform(get("/api/financeiro").headers(unsigned(Identity.create()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature made with another key is rejected")
    void otherKey() throws Exception {
        HttpHeaders forged = signedWith(Identity.create(), "chave-de-atacante-com-mais-de-32-bytes-aqui",
                Instant.now().getEpochSecond());

        mvc.perform(get("/api/financeiro").headers(forged)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature outside the time window is rejected")
    void expiredSignature() throws Exception {
        HttpHeaders old = signedWith(Identity.create(), SIGNING_KEY, Instant.now().minus(Duration.ofMinutes(10)).getEpochSecond());

        mvc.perform(get("/api/financeiro").headers(old)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("changing the organization after signing invalidates the identity")
    void tamperedOrganization() throws Exception {
        HttpHeaders headers = asGateway(Identity.create());
        headers.set("X-Org-Id", newId());

        mvc.perform(get("/api/financeiro").headers(headers)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a secretary cannot use the finance area, a lawyer and an admin can")
    void roles() throws Exception {
        Identity admin = Identity.create();

        getAs(admin.withRole("SECRETARIA"), "/api/financeiro")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        getAs(admin.withRole("ADVOGADO"), "/api/financeiro").andExpect(status().isOk());
        getAs(admin, "/api/financeiro").andExpect(status().isOk());
    }

    @Test
    @DisplayName("a secretary cannot delete an invoice")
    void secretaryCannotDelete() throws Exception {
        Identity admin = Identity.create();
        String invoiceId = createInvoice(admin, newId(), "x", "10.00", "PENDENTE", Instant.now().plus(Duration.ofDays(30)));

        deleteAs(admin.withRole("SECRETARIA"), "/api/financeiro/" + invoiceId).andExpect(status().isForbidden());
        deleteAs(admin.withRole("ADVOGADO"), "/api/financeiro/" + invoiceId).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the internal portal route requires the service key")
    void internalRouteRequiresKey() throws Exception {
        String url = "/internal/portal/honorarios";

        mvc.perform(get(url).param("clientId", newId()).param("orgId", newId())).andExpect(status().isForbidden());
        mvc.perform(get(url).param("clientId", newId()).param("orgId", newId()).header("X-Internal-Key", "wrong-key"))
                .andExpect(status().isForbidden());
        mvc.perform(get(url).param("clientId", newId()).param("orgId", newId()).header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk());
    }
}
