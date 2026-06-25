package app.lexo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integracao ponta-a-ponta da API (app completa sobre H2).
 * Cobre registro, autenticacao obrigatoria, CRUD, validacao e isolamento multi-tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API de clientes (integracao)")
class ClienteApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    /** Registra uma organizacao e devolve o token JWT do admin criado. */
    private String registrarEObterToken(String email) throws Exception {
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

        return om.readTree(resposta).get("token").asText();
    }

    @Test
    @DisplayName("sem token, o acesso e bloqueado")
    void semTokenBloqueia() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("com token, cria e lista cliente")
    void criaEListaCliente() throws Exception {
        String token = registrarEObterToken("cria@teste.com");
        String cliente = om.writeValueAsString(Map.of("name", "Joao da Silva", "document", "52998224725"));

        mvc.perform(post("/api/clientes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Joao da Silva"));

        mvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CPF invalido retorna 400 com mensagem de erro")
    void cpfInvalido() throws Exception {
        String token = registrarEObterToken("cpf@teste.com");
        String cliente = om.writeValueAsString(Map.of("name", "Fulano", "document", "11111111111"));

        mvc.perform(post("/api/clientes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("multi-tenant: uma organizacao nao ve clientes de outra")
    void isolamentoMultiTenant() throws Exception {
        String tokenA = registrarEObterToken("orgA@teste.com");
        String tokenB = registrarEObterToken("orgB@teste.com");

        // Org A cria um cliente
        mvc.perform(post("/api/clientes").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Cliente da A"))))
                .andExpect(status().isCreated());

        // Org B nao deve enxergar nada
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
