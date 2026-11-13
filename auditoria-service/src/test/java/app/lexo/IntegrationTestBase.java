package app.lexo;

import app.lexo.common.event.DomainEvent;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String SIGNING_KEY = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";

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

    @Autowired
    private ApplicationContext context;

    protected record Identity(String userId, String orgId, String role, String name) {

        public static Identity create() {
            return create("ADMIN");
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

    protected ResultActions getAs(Identity id, String url) throws Exception {
        return mvc.perform(get(url).headers(asGateway(id)));
    }

    protected void insertEntry(String orgId, String action, String description, Instant createdAt) {
        jdbc.update("insert into audit_logs (id, organization_id, user_id, user_name, action, entity_type, entity_id, "
                        + "description, created_at, updated_at) values (?, ?, 'u-1', 'Ana Advogada', ?, 'CLIENTE', 'c-1', ?, ?, ?)",
                UUID.randomUUID().toString(), orgId, action, description, Timestamp.from(createdAt),
                Timestamp.from(createdAt));
    }

    protected List<Map<String, Object>> entriesOf(String orgId) {
        return jdbc.queryForList("select * from audit_logs where organization_id = ? order by created_at", orgId);
    }

    protected record Listener(Object bean, Method method, KafkaListener annotation) {
    }

    protected Listener kafkaListener() {
        List<Listener> found = new ArrayList<>();
        for (Object bean : context.getBeansWithAnnotation(Component.class).values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            for (Method method : type.getDeclaredMethods()) {
                KafkaListener annotation = AnnotatedElementUtils.findMergedAnnotation(method, KafkaListener.class);
                if (annotation != null) {
                    method.setAccessible(true);
                    found.add(new Listener(bean, method, annotation));
                }
            }
        }
        if (found.size() != 1) {
            throw new AssertionError("expected exactly one @KafkaListener but found " + found.size());
        }
        return found.get(0);
    }

    protected void deliver(DomainEvent event) throws Exception {
        Listener listener = kafkaListener();
        try {
            listener.method().invoke(listener.bean(), event);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }
}
