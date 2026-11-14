package app.lexo.controller;

import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("Clients API (integration)")
class ClientApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";
    private static final String INTERNAL_KEY = "chave-de-teste-interna";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private record Identity(String userId, String orgId, String role, String name) {
        static Identity create() {
            return create("ADMIN");
        }

        static Identity create(String role) {
            return new Identity(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    role, "Usuario Teste");
        }
    }

    private HttpHeaders asGateway(Identity id) {
        return signed(id, SIGNING_KEY, Instant.now().getEpochSecond());
    }

    private HttpHeaders signed(Identity id, String key, long timestamp) {
        HttpHeaders h = withoutSignature(id);
        String name = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(
                id.userId(), id.orgId(), id.role(), name, "", timestamp);
        h.set("X-User-Name", name);
        h.set("X-User-Email", "");
        h.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        h.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(key, payload));
        return h;
    }

    private HttpHeaders withoutSignature(Identity id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", URLEncoder.encode(id.name(), StandardCharsets.UTF_8));
        h.add("X-User-Email", "");
        return h;
    }

    @Test
    @DisplayName("without identity, access is blocked")
    void noIdentityIsRejected() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("identity headers without the gateway signature are rejected")
    void unsignedHeadersAreRejected() throws Exception {
        mvc.perform(get("/api/clientes").headers(withoutSignature(Identity.create())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature made with another key is rejected")
    void signatureWithOtherKeyIsRejected() throws Exception {
        HttpHeaders forged = signed(Identity.create(),
                "chave-de-atacante-com-mais-de-32-bytes-aqui", Instant.now().getEpochSecond());

        mvc.perform(get("/api/clientes").headers(forged))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a signature outside the time window is rejected")
    void expiredSignatureIsRejected() throws Exception {
        HttpHeaders old = signed(Identity.create(), SIGNING_KEY,
                Instant.now().getEpochSecond() - 600);

        mvc.perform(get("/api/clientes").headers(old))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("changing the role after signing invalidates the identity")
    void tamperedRoleIsRejected() throws Exception {
        Identity id = Identity.create();
        HttpHeaders h = asGateway(id);
        h.set("X-User-Role", "SECRETARIA");

        mvc.perform(get("/api/clientes").headers(h))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("with identity, creates and lists a client")
    void createsAndListsClient() throws Exception {
        Identity id = Identity.create();
        String client = om.writeValueAsString(Map.of("name", "Joao da Silva", "document", "52998224725"));

        mvc.perform(post("/api/clientes").headers(asGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(client))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Joao da Silva"));

        mvc.perform(get("/api/clientes").headers(asGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("an invalid CPF returns 400 with message and error code")
    void invalidCpf() throws Exception {
        Identity id = Identity.create();
        String client = om.writeValueAsString(Map.of("name", "Fulano", "document", "11111111111"));

        mvc.perform(post("/api/clientes").headers(asGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(client))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("multi-tenant: one organization does not see clients of another")
    void multiTenantIsolation() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();

        mvc.perform(post("/api/clientes").headers(asGateway(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Cliente da A"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clientes").headers(asGateway(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("a secretary cannot delete a client")
    void secretaryCannotDelete() throws Exception {
        Identity admin = Identity.create("ADMIN");
        String client = om.writeValueAsString(Map.of("name", "Cliente X"));
        String id = om.readTree(mvc.perform(post("/api/clientes").headers(asGateway(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(client))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        Identity secretary = new Identity(UUID.randomUUID().toString(), admin.orgId(), "SECRETARIA", "Secretaria Teste");

        mvc.perform(delete("/api/clientes/{id}", id).headers(asGateway(secretary)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("lawyer and admin can delete a client (role hierarchy)")
    void lawyerAndAdminCanDelete() throws Exception {
        Identity admin = Identity.create("ADMIN");

        String lawyerClient = om.readTree(mvc.perform(post("/api/clientes").headers(asGateway(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("name", "Cliente Advogado"))))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        Identity lawyer = new Identity(UUID.randomUUID().toString(), admin.orgId(), "ADVOGADO", "Advogado Teste");
        mvc.perform(delete("/api/clientes/{id}", lawyerClient).headers(asGateway(lawyer)))
                .andExpect(status().isNoContent());

        String adminClient = om.readTree(mvc.perform(post("/api/clientes").headers(asGateway(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("name", "Cliente Admin"))))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(delete("/api/clientes/{id}", adminClient).headers(asGateway(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the internal route requires the service key")
    void internalRouteRequiresServiceKey() throws Exception {
        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString())
                        .header("X-Internal-Key", "chave-errada"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/internal/clientes/{id}/existe", UUID.randomUUID().toString())
                        .param("orgId", UUID.randomUUID().toString())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }
}
