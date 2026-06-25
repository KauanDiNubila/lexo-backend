package app.lexo.controller;

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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integracao do monolito (clientes) sobre H2. A identidade vem dos headers
 * X-User-* injetados pelo gateway — entao os testes fabricam essa identidade diretamente
 * (o auth/registro agora vive no auth-service, fora deste servico).
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
        static Identidade nova() {
            return new Identidade(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    "ADMIN", "Admin Teste");
        }
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
        Identidade id = Identidade.nova();
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
        Identidade id = Identidade.nova();
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
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();

        mvc.perform(post("/api/clientes").headers(comoGateway(a))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Cliente da A"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clientes").headers(comoGateway(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
