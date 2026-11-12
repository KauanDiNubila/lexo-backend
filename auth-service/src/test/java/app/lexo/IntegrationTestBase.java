package app.lexo;

import app.lexo.common.event.DomainEvent;
import app.lexo.common.kafka.EventPublisher;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.servlet.http.Cookie;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
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
    protected static final String AUTH_SECRET = "segredo-de-teste-do-auth-com-mais-de-32-bytes";
    protected static final String PASSWORD = "senha-forte-123";
    protected static final String REFRESH_COOKIE = "lexo_refresh_token";

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

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockitoBean
    protected EventPublisher events;

    @MockitoBean
    protected RabbitTemplate rabbit;

    @MockitoBean
    protected StringRedisTemplate redis;

    protected record Account(String userId, String orgId, String name, String email, String role,
                             String accessToken, String refreshToken) {

        Account withRole(String newRole) {
            return new Account(userId, orgId, name, email, newRole, accessToken, refreshToken);
        }
    }

    protected Account ghost(String orgId) {
        return new Account(UUID.randomUUID().toString(), orgId, "Fantasma", "ghost@escritorio.com", "ADMIN",
                null, null);
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@escritorio.com";
    }

    protected Account registerAdmin() throws Exception {
        return registerAdmin(uniqueEmail());
    }

    protected Account registerAdmin(String email) throws Exception {
        ResultActions response = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "organizationName", "Escritorio Teste", "name", "Ana Admin", "email", email,
                        "password", PASSWORD, "confirmPassword", PASSWORD))))
                .andExpect(status().isOk());
        JsonNode body = bodyOf(response);
        Cookie refresh = response.andReturn().getResponse().getCookie(REFRESH_COOKIE);
        JsonNode user = body.get("user");
        return new Account(user.get("id").asText(), user.get("organizationId").asText(), user.get("name").asText(),
                user.get("email").asText(), user.get("role").asText(), body.get("accessToken").asText(),
                refresh == null ? null : refresh.getValue());
    }

    protected Account insertMember(Account admin, String role) {
        return insertMember(admin.orgId(), "Membro " + role, uniqueEmail(), role);
    }

    protected Account insertMember(String orgId, String name, String email, String role) {
        String id = UUID.randomUUID().toString();
        jdbc.update("insert into users (id, organization_id, name, email, password_hash, role, totp_enabled, "
                        + "created_at, updated_at) values (?, ?, ?, ?, ?, ?, false, now(), now())",
                id, orgId, name, email, passwordEncoder.encode(PASSWORD), role);
        return new Account(id, orgId, name, email, role, null, null);
    }

    protected HttpHeaders asGateway(Account account) {
        return signedWith(account, SIGNING_KEY, Instant.now().getEpochSecond());
    }

    protected HttpHeaders signedWith(Account account, String key, long timestamp) {
        HttpHeaders headers = unsigned(account);
        String name = URLEncoder.encode(account.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(account.userId(), account.orgId(), account.role(), name,
                account.email(), timestamp);
        headers.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        headers.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(key, payload));
        return headers;
    }

    protected HttpHeaders unsigned(Account account) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", account.userId());
        headers.add("X-Org-Id", account.orgId());
        headers.add("X-User-Role", account.role());
        headers.add("X-User-Name", URLEncoder.encode(account.name(), StandardCharsets.UTF_8));
        headers.add("X-User-Email", account.email());
        return headers;
    }

    protected ResultActions getAs(Account account, String url) throws Exception {
        return mvc.perform(get(url).headers(asGateway(account)));
    }

    protected ResultActions postAs(Account account, String url, Object body) throws Exception {
        return mvc.perform(withBody(post(url).headers(asGateway(account)), body));
    }

    protected ResultActions putAs(Account account, String url, Object body) throws Exception {
        return mvc.perform(withBody(put(url).headers(asGateway(account)), body));
    }

    protected ResultActions patchAs(Account account, String url, Object body) throws Exception {
        return mvc.perform(withBody(patch(url).headers(asGateway(account)), body));
    }

    protected ResultActions deleteAs(Account account, String url) throws Exception {
        return mvc.perform(delete(url).headers(asGateway(account)));
    }

    protected ResultActions postPublic(String url, Object body) throws Exception {
        return mvc.perform(withBody(post(url), body));
    }

    protected MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    protected ResultActions login(String email, String password, String totpCode) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("totpCode", totpCode);
        return postPublic("/api/auth/login", body);
    }

    protected ResultActions refreshWith(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)));
    }

    protected JsonNode bodyOf(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsString());
    }

    protected String totpCodeFor(String secret) throws Exception {
        long counter = Math.floorDiv(new SystemTimeProvider().getTime(), 30L);
        return new DefaultCodeGenerator().generate(secret, counter);
    }

    protected String wrongTotpCode(String secret) throws Exception {
        long counter = Math.floorDiv(new SystemTimeProvider().getTime(), 30L);
        DefaultCodeGenerator generator = new DefaultCodeGenerator();
        List<String> valid = List.of(generator.generate(secret, counter - 1), generator.generate(secret, counter),
                generator.generate(secret, counter + 1));
        for (int candidate = 0; candidate < 1_000_000; candidate += 7919) {
            String code = String.format("%06d", candidate);
            if (!valid.contains(code)) {
                return code;
            }
        }
        throw new IllegalStateException("no wrong code found");
    }

    protected String enableTwoFactor(Account account) throws Exception {
        String secret = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of()).andExpect(status().isOk()))
                .get("secret").asText();
        postAs(account, "/api/2fa/confirmar", Map.of("code", totpCodeFor(secret)))
                .andExpect(status().isNoContent());
        return secret;
    }

    protected List<DomainEvent> publishedEvents() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events, atLeast(0)).publish(captor.capture());
        return captor.getAllValues();
    }

    protected DomainEvent onlyPublishedEvent() {
        List<DomainEvent> published = publishedEvents();
        if (published.size() != 1) {
            throw new AssertionError("expected one event but got " + published);
        }
        return published.get(0);
    }
}
