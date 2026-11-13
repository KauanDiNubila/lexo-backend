package app.lexo;

import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    protected static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";

    protected static final GeminiStub GEMINI = new GeminiStub();

    @DynamicPropertySource
    static void geminiStub(DynamicPropertyRegistry registry) {
        registry.add("lexo.ia.gemini.base-url", GEMINI::baseUrl);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    @MockitoBean
    protected StringRedisTemplate redis;

    @BeforeEach
    void freshStubs() {
        GEMINI.reset();
        reset(redis);
        redisCounts(1L);
    }

    protected void redisCounts(Long count) {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenReturn(count);
    }

    protected record Identity(String userId, String orgId, String role, String name) {

        public static Identity create() {
            return create("ADVOGADO");
        }

        public static Identity create(String role) {
            return new Identity(UUID.randomUUID().toString(), UUID.randomUUID().toString(), role, "Ana Advogada");
        }
    }

    protected HttpHeaders asGateway(Identity id) {
        long timestamp = Instant.now().getEpochSecond();
        String name = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(id.userId(), id.orgId(), id.role(), name, "", timestamp);
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", name);
        h.add("X-User-Email", "");
        h.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        h.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(SIGNING_KEY, payload));
        return h;
    }

    protected ResultActions postAs(Identity id, String url, Object body) throws Exception {
        return mvc.perform(post(url).headers(asGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body)));
    }

    protected JsonNode json(String text) throws Exception {
        return om.readTree(text);
    }
}
