package app.lexo;

import app.lexo.client.AuthServiceClient;
import app.lexo.client.ClienteServiceClient;
import app.lexo.common.email.EnfileiradorEmail;
import app.lexo.common.kafka.PublicadorEventos;
import app.lexo.common.referencia.ExisteResponse;
import app.lexo.common.signing.GatewaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @MockitoBean
    protected ClienteServiceClient clienteClient;

    @MockitoBean
    protected AuthServiceClient authClient;

    @MockitoBean
    protected PublicadorEventos eventos;

    @MockitoBean
    protected EnfileiradorEmail email;

    @MockitoBean
    protected StringRedisTemplate redis;

    protected record Identidade(String userId, String orgId, String role, String name) {

        public static Identidade nova() {
            return nova("ADMIN");
        }

        public static Identidade nova(String role) {
            return new Identidade(UUID.randomUUID().toString(), UUID.randomUUID().toString(), role, "Usuario Teste");
        }

        public Identidade comPapel(String novoPapel) {
            return new Identidade(userId, orgId, novoPapel, name);
        }
    }

    @BeforeEach
    void clienteEResponsavelExistem() {
        when(clienteClient.clienteExiste(anyString(), anyString())).thenReturn(new ExisteResponse(true));
        when(authClient.usuarioExiste(anyString(), anyString())).thenReturn(new ExisteResponse(true));
    }

    protected HttpHeaders comoGateway(Identidade id) {
        return assinado(id, CHAVE_ASSINATURA, Instant.now().getEpochSecond());
    }

    protected HttpHeaders assinado(Identidade id, String chave, long timestamp) {
        HttpHeaders h = semAssinatura(id);
        String nome = URLEncoder.encode(id.name(), StandardCharsets.UTF_8);
        String payload = GatewaySignature.payload(id.userId(), id.orgId(), id.role(), nome, "", timestamp);
        h.add(GatewaySignature.H_TIMESTAMP, Long.toString(timestamp));
        h.add(GatewaySignature.H_ASSINATURA, GatewaySignature.sign(chave, payload));
        return h;
    }

    protected HttpHeaders semAssinatura(Identidade id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", URLEncoder.encode(id.name(), StandardCharsets.UTF_8));
        h.add("X-User-Email", "");
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

    protected ResultActions alternarStatus(Identidade id, String prazoId, boolean concluido) throws Exception {
        return mvc.perform(patch("/api/agenda/{id}/status", prazoId).headers(comoGateway(id))
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(Map.of("completed", concluido))));
    }

    protected ResultActions buscar(Identidade id, String url) throws Exception {
        return mvc.perform(get(url).headers(comoGateway(id)));
    }

    protected ResultActions apagar(Identidade id, String url) throws Exception {
        return mvc.perform(delete(url).headers(comoGateway(id)));
    }

    protected String idDe(ResultActions resposta) throws Exception {
        return corpoDe(resposta).get("id").asText();
    }

    protected JsonNode corpoDe(ResultActions resposta) throws Exception {
        return om.readTree(resposta.andReturn().getResponse().getContentAsString());
    }

    protected Map<String, Object> dadosDoProcesso(String clienteId, String numero) {
        return Map.of("clientId", clienteId, "number", numero, "area", "Civel", "status", "ATIVO");
    }

    protected String criarProcesso(Identidade id, String clienteId, String numero) throws Exception {
        return idDe(postar(id, "/api/processos", dadosDoProcesso(clienteId, numero)).andExpect(status().isCreated()));
    }

    protected String criarPrazo(Identidade id, String processoId, String tipo, Duration quando) throws Exception {
        Map<String, Object> prazo = Map.of("caseId", processoId, "title", "Prazo " + UUID.randomUUID(),
                "type", tipo, "date", Instant.now().plus(quando).toString());
        return idDe(postar(id, "/api/agenda", prazo).andExpect(status().isCreated()));
    }

    protected String novoId() {
        return UUID.randomUUID().toString();
    }
}
