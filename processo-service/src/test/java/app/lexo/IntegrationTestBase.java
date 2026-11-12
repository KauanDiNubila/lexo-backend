package app.lexo;

import app.lexo.client.AuthServiceClient;
import app.lexo.client.ClienteServiceClient;
import app.lexo.common.kafka.EventPublisher;
import app.lexo.common.reference.ExistsResponse;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected ClienteServiceClient clientService;

    @MockitoBean
    protected AuthServiceClient authService;

    @MockitoBean
    protected EventPublisher events;

    @MockitoBean
    protected RabbitTemplate rabbit;

    @MockitoBean
    protected StringRedisTemplate redis;

    protected record Identity(String userId, String orgId, String role, String name) {

        public static Identity create() {
            return create("ADMIN");
        }

        public static Identity create(String role) {
            return new Identity(UUID.randomUUID().toString(), UUID.randomUUID().toString(), role, "Test User");
        }

        public Identity withRole(String newRole) {
            return new Identity(userId, orgId, newRole, name);
        }
    }

    @BeforeEach
    void clientAndResponsibleExist() {
        when(clientService.clienteExiste(anyString(), anyString())).thenReturn(new ExistsResponse(true));
        when(authService.usuarioExiste(anyString(), anyString())).thenReturn(new ExistsResponse(true));
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
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected ResultActions putAs(Identity id, String url, Object body) throws Exception {
        return mvc.perform(put(url).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected ResultActions toggleStatus(Identity id, String deadlineId, boolean completed) throws Exception {
        return mvc.perform(patch("/api/agenda/{id}/status", deadlineId).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("completed", completed))));
    }

    protected ResultActions getAs(Identity id, String url) throws Exception {
        return mvc.perform(get(url).headers(asGateway(id)));
    }

    protected ResultActions deleteAs(Identity id, String url) throws Exception {
        return mvc.perform(delete(url).headers(asGateway(id)));
    }

    protected String idOf(ResultActions response) throws Exception {
        return bodyOf(response).get("id").asText();
    }

    protected JsonNode bodyOf(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsString());
    }

    protected Map<String, Object> caseData(String clientId, String number) {
        return Map.of("clientId", clientId, "number", number, "area", "Civel", "status", "ATIVO");
    }

    protected String createCase(Identity id, String clientId, String number) throws Exception {
        return idOf(postAs(id, "/api/processos", caseData(clientId, number)).andExpect(status().isCreated()));
    }

    protected String createDeadline(Identity id, String caseId, String type, Duration when) throws Exception {
        Map<String, Object> deadline = Map.of("caseId", caseId, "title", "Deadline " + UUID.randomUUID(),
                "type", type, "date", Instant.now().plus(when).toString());
        return idOf(postAs(id, "/api/agenda", deadline).andExpect(status().isCreated()));
    }

    protected String createMovement(Identity id, String caseId, String title, Instant date) throws Exception {
        return idOf(postAs(id, "/api/processos/" + caseId + "/andamentos",
                Map.of("title", title, "description", "details of " + title, "date", date.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(caseId)));
    }

    protected String newId() {
        return UUID.randomUUID().toString();
    }
}
