package app.lexo;

import app.lexo.client.FinanceiroPortalClient;
import app.lexo.client.ProcessoPortalClient;
import app.lexo.common.kafka.PublicadorEventos;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracaoBase {

    protected static final String CHAVE_ASSINATURA = "chave-de-assinatura-de-teste-com-mais-de-32-bytes";
    protected static final String CHAVE_INTERNA = "chave-de-teste-interna";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper om;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected ProcessoPortalClient processoClient;

    @MockitoBean
    protected FinanceiroPortalClient financeiroClient;

    @MockitoBean
    protected PublicadorEventos eventos;

    protected record Identidade(String userId, String orgId, String role, String name) {

        public static Identidade nova() {
            return nova("ADMIN");
        }

        public static Identidade nova(String role) {
            return new Identidade(UUID.randomUUID().toString(), UUID.randomUUID().toString(), role, "Ana Advogada");
        }

        public Identidade comPapel(String novoPapel) {
            return new Identidade(userId, orgId, novoPapel, name);
        }
    }

    protected HttpHeaders comoGateway(Identidade id) {
        long timestamp = Instant.now().getEpochSecond();
        String nome = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(id.userId(), id.orgId(), id.role(), nome, "", timestamp);
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", nome);
        h.add("X-User-Email", "");
        h.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        h.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(CHAVE_ASSINATURA, payload));
        return h;
    }

    protected ResultActions postar(Identidade id, String url, Object corpo) throws Exception {
        return mvc.perform(post(url).headers(comoGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(corpo)));
    }

    protected ResultActions colocar(Identidade id, String url, Object corpo) throws Exception {
        return mvc.perform(put(url).headers(comoGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(corpo)));
    }

    protected ResultActions buscar(Identidade id, String url) throws Exception {
        return mvc.perform(get(url).headers(comoGateway(id)));
    }

    protected ResultActions apagar(Identidade id, String url) throws Exception {
        return mvc.perform(delete(url).headers(comoGateway(id)));
    }

    protected JsonNode corpoDe(ResultActions resposta) throws Exception {
        return om.readTree(resposta.andReturn().getResponse().getContentAsString());
    }

    protected String idDe(ResultActions resposta) throws Exception {
        return corpoDe(resposta).get("id").asText();
    }

    protected String criarCliente(Identidade id, String nome) throws Exception {
        return idDe(postar(id, "/api/clientes", Map.of("name", nome)).andExpect(status().isCreated()));
    }

    protected String novoId() {
        return UUID.randomUUID().toString();
    }
}
