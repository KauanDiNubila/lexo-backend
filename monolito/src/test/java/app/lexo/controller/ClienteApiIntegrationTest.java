package app.lexo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integracao ponta-a-ponta da API (app completa sobre H2).
 * Como agora a identidade vem dos headers X-User-* (injetados pelo gateway), os testes
 * simulam o gateway enviando esses headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API de clientes (integracao)")
class ClienteApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    /** Identidade que o gateway repassaria via headers. */
    private record Identidade(String userId, String orgId, String role, String name) {
    }

    /** Registra uma organizacao e devolve a identidade do admin criado. */
    private Identidade registrar(String email) throws Exception {
        String body = om.writeValueAsString(Map.of(
                "organizationName", "Escritorio " + email,
                "name", "Admin Teste",
                "email", email,
                "password", "senha12345",
                "confirmPassword", "senha12345"));

        String resposta = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode user = om.readTree(resposta).get("user");
        return new Identidade(user.get("id").asText(), user.get("organizationId").asText(),
                user.get("role").asText(), user.get("name").asText());
    }

    /** Monta os headers de confianca, como o gateway faria apos validar o JWT. */
    private HttpHeaders comoGateway(Identidade id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Id", id.userId());
        h.add("X-Org-Id", id.orgId());
        h.add("X-User-Role", id.role());
        h.add("X-User-Name", id.name());
        return h;
    }

    @Test
    @DisplayName("sem identidade, o acesso e bloqueado")
    void semIdentidadeBloqueia() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("com identidade, cria e lista cliente")
    void criaEListaCliente() throws Exception {
        Identidade id = registrar("cria@teste.com");
        String cliente = om.writeValueAsString(Map.of("name", "Joao da Silva", "document", "52998224725"));

        mvc.perform(post("/api/clientes").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Joao da Silva"));

        mvc.perform(get("/api/clientes").headers(comoGateway(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CPF invalido retorna 400 com mensagem de erro")
    void cpfInvalido() throws Exception {
        Identidade id = registrar("cpf@teste.com");
        String cliente = om.writeValueAsString(Map.of("name", "Fulano", "document", "11111111111"));

        mvc.perform(post("/api/clientes").headers(comoGateway(id))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("multi-tenant: uma organizacao nao ve clientes de outra")
    void isolamentoMultiTenant() throws Exception {
        Identidade a = registrar("orgA@teste.com");
        Identidade b = registrar("orgB@teste.com");

        mvc.perform(post("/api/clientes").headers(comoGateway(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Cliente da A"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clientes").headers(comoGateway(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
