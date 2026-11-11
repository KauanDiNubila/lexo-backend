package app.lexo;

import app.lexo.adapter.out.cases.CaseServiceClient;
import app.lexo.adapter.out.client.ClientServiceClient;
import app.lexo.common.referencia.ExisteResponse;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    protected static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";
    protected static final String INTERNAL_KEY = "chave-de-teste-interna";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected ClientServiceClient clientService;

    @MockitoBean
    protected CaseServiceClient caseService;

    protected record Identity(String userId, String orgId, String role, String name) {

        public static Identity create() {
            return create("ADMIN");
        }

        public static Identity create(String role) {
            return new Identity(UUID.randomUUID().toString(), UUID.randomUUID().toString(), role, "Ana Advogada");
        }

        public Identity withRole(String newRole) {
            return new Identity(userId, orgId, newRole, name);
        }
    }

    @BeforeEach
    void referencesExist() {
        when(clientService.clientExists(anyString(), anyString())).thenReturn(new ExisteResponse(true));
        when(caseService.caseExists(anyString(), anyString())).thenReturn(new ExisteResponse(true));
    }

    protected HttpHeaders asGateway(Identity id) {
        return signedWith(id, SIGNING_KEY, Instant.now().getEpochSecond());
    }

    protected HttpHeaders signedWith(Identity id, String key, long timestamp) {
        HttpHeaders headers = unsigned(id);
        String name = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(id.userId(), id.orgId(), id.role(), name, "", timestamp);
        headers.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        headers.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(key, payload));
        return headers;
    }

    protected HttpHeaders unsigned(Identity id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", id.userId());
        headers.add("X-Org-Id", id.orgId());
        headers.add("X-User-Role", id.role());
        headers.add("X-User-Name", URLEncoder.encode(id.name(), StandardCharsets.UTF_8));
        headers.add("X-User-Email", "");
        return headers;
    }

    protected ResultActions postAs(Identity id, String url, Object body) throws Exception {
        return mvc.perform(post(url).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }

    protected ResultActions putAs(Identity id, String url, Object body) throws Exception {
        return mvc.perform(put(url).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }

    protected ResultActions patchAs(Identity id, String url, Object body) throws Exception {
        return mvc.perform(patch(url).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }

    protected ResultActions getAs(Identity id, String url) throws Exception {
        return mvc.perform(get(url).headers(asGateway(id)));
    }

    protected ResultActions deleteAs(Identity id, String url) throws Exception {
        return mvc.perform(delete(url).headers(asGateway(id)));
    }

    protected JsonNode bodyOf(ResultActions response) throws Exception {
        return om.readTree(response.andReturn().getResponse().getContentAsString());
    }

    protected Map<String, Object> invoiceData(String clientId, String description, String amount, String status,
                                              Instant dueDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", clientId);
        body.put("description", description);
        body.put("amount", new BigDecimal(amount));
        body.put("status", status);
        body.put("dueDate", dueDate.toString());
        return body;
    }

    protected String createInvoice(Identity id, String clientId, String description, String amount, String status,
                                   Instant dueDate) throws Exception {
        ResultActions response = postAs(id, "/api/financeiro", invoiceData(clientId, description, amount, status, dueDate))
                .andExpect(status().isCreated());
        return bodyOf(response).get("id").asText();
    }

    protected String newId() {
        return UUID.randomUUID().toString();
    }
}
