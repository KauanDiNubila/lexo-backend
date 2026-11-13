package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import app.lexo.common.portal.PortalDtos.PortalFee;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.domain.PortalToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Clients API: behavior pinned before the hexagonal migration")
class ClientApiBehaviorTest extends IntegrationTestBase {

    private Map<String, Object> data(String name, String taxId, String email, String phone) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("document", taxId);
        data.put("email", email);
        data.put("phone", phone);
        return data;
    }

    @Test
    @DisplayName("create returns the full client and publishes CLIENTE_CRIADO")
    void createPublishesEvent() throws Exception {
        Identity id = Identity.create();

        String clientId = idOf(postAs(id, "/api/clientes", data("Maria Souza", "529.982.247-25", "maria@escritorio.com", "11999990000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.document").value("529.982.247-25"))
                .andExpect(jsonPath("$.email").value("maria@escritorio.com"))
                .andExpect(jsonPath("$.phone").value("11999990000"))
                .andExpect(jsonPath("$.portalAtivo").value(false))
                .andExpect(jsonPath("$.portalExpiraEm").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").exists()));

        verify(events).publish(argThat(e -> "CLIENTE_CRIADO".equals(e.type())
                && id.orgId().equals(e.organizationId())
                && "CLIENTE".equals(e.entityType())
                && clientId.equals(e.entityId())
                && "Cliente criado: Maria Souza".equals(e.description())
                && id.userId().equals(e.userId())
                && "Ana Advogada".equals(e.userName())));
    }

    @Test
    @DisplayName("blank optional fields become null")
    void blankFieldsBecomeNull() throws Exception {
        Identity id = Identity.create();

        postAs(id, "/api/clientes", data("Joao Lima", "", "  ", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(nullValue()));
    }

    @Test
    @DisplayName("a short name returns 400 with the validation message")
    void shortName() throws Exception {
        Identity id = Identity.create();

        postAs(id, "/api/clientes", Map.of("name", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error", containsString("Nome muito curto")));
    }

    @Test
    @DisplayName("update changes the fields, publishes CLIENTE_ATUALIZADO and another organization gets 404")
    void update() throws Exception {
        Identity id = Identity.create();
        Identity other = Identity.create();
        String clientId = createClient(id, "Maria Souza");

        putAs(id, "/api/clientes/" + clientId, data("Maria Souza Lima", "52998224725", "novo@escritorio.com", "11888880000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.name").value("Maria Souza Lima"))
                .andExpect(jsonPath("$.document").value("52998224725"));

        getAs(id, "/api/clientes/" + clientId)
                .andExpect(jsonPath("$.email").value("novo@escritorio.com"))
                .andExpect(jsonPath("$.phone").value("11888880000"));
        verify(events).publish(argThat(e -> "CLIENTE_ATUALIZADO".equals(e.type())
                && id.orgId().equals(e.organizationId())
                && "CLIENTE".equals(e.entityType())
                && clientId.equals(e.entityId())
                && "Cliente atualizado: Maria Souza Lima".equals(e.description())
                && id.userId().equals(e.userId())
                && "Ana Advogada".equals(e.userName())));

        putAs(other, "/api/clientes/" + clientId, data("Invasor", null, null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado"))
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));
        verify(events, times(2)).publish(any());
        getAs(other, "/api/clientes/" + clientId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("updating with an invalid CPF returns 400 before looking up the client")
    void updateWithInvalidDocument() throws Exception {
        Identity id = Identity.create();

        putAs(id, "/api/clientes/" + newId(), data("Fulano Tal", "11111111111", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"));
    }

    @Test
    @DisplayName("delete publishes CLIENTE_EXCLUIDO and a second delete returns 404")
    void delete() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Carlos Dias");

        deleteAs(id, "/api/clientes/" + clientId).andExpect(status().isNoContent());

        verify(events).publish(argThat(e -> "CLIENTE_EXCLUIDO".equals(e.type())
                && clientId.equals(e.entityId())
                && "Cliente excluído: Carlos Dias".equals(e.description())));
        deleteAs(id, "/api/clientes/" + clientId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("another organization cannot delete the client")
    void deleteFromOtherOrganization() throws Exception {
        Identity owner = Identity.create();
        Identity other = Identity.create();
        String clientId = createClient(owner, "Carlos Dias");

        deleteAs(other, "/api/clientes/" + clientId).andExpect(status().isNotFound());

        getAs(owner, "/api/clientes/" + clientId).andExpect(status().isOk());
    }

    @Test
    @DisplayName("lists by name, paginates, sorts and searches name, document, e-mail and phone")
    void listing() throws Exception {
        Identity id = Identity.create();
        postAs(id, "/api/clientes", data("Carla Mendes", "529.982.247-25", "carla@x.com", "11911110000")).andExpect(status().isCreated());
        postAs(id, "/api/clientes", data("Ana Paula", "", "ANA@LOJA.COM", "21922220000")).andExpect(status().isCreated());
        postAs(id, "/api/clientes", data("Bruno Reis", "", "", "31933330000")).andExpect(status().isCreated());

        getAs(id, "/api/clientes")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Ana Paula"))
                .andExpect(jsonPath("$.content[2].name").value("Carla Mendes"));
        getAs(id, "/api/clientes?sort=name,desc")
                .andExpect(jsonPath("$.content[0].name").value("Carla Mendes"));
        getAs(id, "/api/clientes?size=2&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        getAs(id, "/api/clientes?q=BRUNO").andExpect(jsonPath("$.totalElements").value(1));
        getAs(id, "/api/clientes?q=529.982").andExpect(jsonPath("$.content[0].name").value("Carla Mendes"));
        getAs(id, "/api/clientes?q=ana@loja").andExpect(jsonPath("$.content[0].name").value("Ana Paula"));
        getAs(id, "/api/clientes?q=9333").andExpect(jsonPath("$.content[0].name").value("Bruno Reis"));
        getAs(id, "/api/clientes?q=  ").andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("options lists id and name alphabetically and summary counts the clients")
    void optionsAndSummary() throws Exception {
        Identity id = Identity.create();
        String zeca = createClient(id, "Zeca Alves");
        String ana = createClient(id, "Ana Paula");

        getAs(id, "/api/clientes/opcoes")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ana))
                .andExpect(jsonPath("$[0].name").value("Ana Paula"))
                .andExpect(jsonPath("$[1].id").value(zeca));
        getAs(id, "/api/clientes/resumo").andExpect(jsonPath("$.total").value(2));
        getAs(Identity.create(), "/api/clientes/resumo").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("the portal generates the link, aggregates cases and fees, and a new link invalidates the previous one")
    void portal() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        when(caseClient.casesOfClient(eq(clientId), eq(id.orgId()))).thenReturn(List.of(
                new PortalCase("P-1", "Civel", "ATIVO", Instant.now(), List.of(), List.of())));
        when(financeClient.feesOfClient(eq(clientId), eq(id.orgId()))).thenReturn(List.of(
                new PortalFee("Consulta", new BigDecimal("100.00"), "PENDENTE", Instant.now()),
                new PortalFee("Audiencia", new BigDecimal("50.50"), "ATRASADO", Instant.now()),
                new PortalFee("Peticao", new BigDecimal("200.00"), "PAGO", Instant.now())));

        String first = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of()).andExpect(status().isOk()))
                .get("token").asText();
        assertThat(first).hasSize(64);
        getAs(id, "/api/clientes/" + clientId)
                .andExpect(jsonPath("$.portalAtivo").value(true))
                .andExpect(jsonPath("$.portalExpiraEm").exists());

        mvc.perform(get("/api/portal/{token}", first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Helena Prado"))
                .andExpect(jsonPath("$.processos.length()").value(1))
                .andExpect(jsonPath("$.processos[0].number").value("P-1"))
                .andExpect(jsonPath("$.honorarios.length()").value(3))
                .andExpect(jsonPath("$.resumo.totalProcessos").value(1))
                .andExpect(jsonPath("$.resumo.emAberto").value(150.5));

        String second = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();
        assertThat(second).isNotEqualTo(first);
        mvc.perform(get("/api/portal/{token}", first))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));
        mvc.perform(get("/api/portal/{token}", second)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("revoke removes the link and the client no longer has an active portal")
    void revokePortal() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        String token = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();

        deleteAs(id, "/api/clientes/" + clientId + "/portal").andExpect(status().isNoContent());

        mvc.perform(get("/api/portal/{token}", token)).andExpect(status().isNotFound());
        getAs(id, "/api/clientes/" + clientId)
                .andExpect(jsonPath("$.portalAtivo").value(false))
                .andExpect(jsonPath("$.portalExpiraEm").value(nullValue()));
    }

    @Test
    @DisplayName("an expired link returns 404")
    void expiredPortal() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        String token = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();
        jdbc.update("update clients set portal_token_expires_at = now() - interval '1 day' where id = ?", clientId);

        mvc.perform(get("/api/portal/{token}", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));
        getAs(id, "/api/clientes/" + clientId).andExpect(jsonPath("$.portalAtivo").value(false));
    }

    @Test
    @DisplayName("generating or revoking the portal of another organization returns 404")
    void portalFromOtherOrganization() throws Exception {
        Identity owner = Identity.create();
        Identity other = Identity.create();
        String clientId = createClient(owner, "Helena Prado");

        postAs(other, "/api/clientes/" + clientId + "/portal", Map.of()).andExpect(status().isNotFound());
        deleteAs(other, "/api/clientes/" + clientId + "/portal").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("if the cases service fails, the portal answers 503 instead of an empty section")
    void portalWithCasesServiceDown() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        String token = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();
        when(caseClient.casesOfClient(anyString(), anyString())).thenThrow(new RuntimeException("fora do ar"));
        when(financeClient.feesOfClient(anyString(), anyString())).thenReturn(List.of(
                new PortalFee("Consulta", new BigDecimal("80.00"), "PENDENTE", Instant.now())));

        mvc.perform(get("/api/portal/{token}", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"))
                .andExpect(jsonPath("$.error").value("Não foi possível carregar o portal agora. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("if the finance service fails, the portal answers 503 instead of reporting nothing outstanding")
    void portalWithFinanceServiceDown() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        String token = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();
        when(caseClient.casesOfClient(anyString(), anyString())).thenReturn(List.of());
        when(financeClient.feesOfClient(anyString(), anyString())).thenThrow(new RuntimeException("fora do ar"));

        mvc.perform(get("/api/portal/{token}", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"));
    }

    @Test
    @DisplayName("the database keeps only the hash of the portal token")
    void portalTokenIsStoredHashed() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");

        String token = bodyOf(postAs(id, "/api/clientes/" + clientId + "/portal", Map.of())).get("token").asText();

        String stored = jdbc.queryForObject("select portal_token_hash from clients where id = ?", String.class, clientId);
        assertThat(stored).isEqualTo(PortalToken.hash(token)).isNotEqualTo(token);
        assertThat(jdbc.queryForObject("select count(*) from clients where portal_token_hash = ?", Integer.class, token))
                .isZero();
    }

    @Test
    @DisplayName("migration V3 hashes the tokens stored in plain text so old links keep working")
    void legacyPlainTokenAfterMigration() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");
        String plain = "legado".repeat(10);
        String script = new String(new ClassPathResource("db/migration/V3__portal_token_hash.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        jdbc.execute("alter table clients rename column portal_token_hash to portal_token");
        try {
            jdbc.update("update clients set portal_token = ?, portal_token_expires_at = now() + interval '1 day' where id = ?",
                    plain, clientId);
            jdbc.execute(script);
        } finally {
            Integer legacyColumns = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_name = 'clients' and column_name = 'portal_token'",
                    Integer.class);
            if (legacyColumns != null && legacyColumns > 0) {
                jdbc.execute("alter table clients rename column portal_token to portal_token_hash");
            }
        }
        when(caseClient.casesOfClient(anyString(), anyString())).thenReturn(List.of());
        when(financeClient.feesOfClient(anyString(), anyString())).thenReturn(List.of());

        mvc.perform(get("/api/portal/{token}", plain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Helena Prado"));
        assertThat(jdbc.queryForObject("select portal_token_hash from clients where id = ?", String.class, clientId))
                .isEqualTo(PortalToken.hash(plain));
    }

    @Test
    @DisplayName("sorting by a property that does not exist returns 500, as before")
    void sortByUnknownProperty() throws Exception {
        Identity id = Identity.create();
        createClient(id, "Maria Souza");

        getAs(id, "/api/clientes?sort=inexistente,asc")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERRO_INTERNO"));
    }

    @Test
    @DisplayName("/internal/clientes/{id}/existe with a blank orgId answers false")
    void internalExistsWithBlankOrganization() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");

        mvc.perform(get("/internal/clientes/{id}/existe", clientId).param("orgId", "")
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    @DisplayName("/internal/clientes/{id}/existe answers per organization")
    void internalExists() throws Exception {
        Identity id = Identity.create();
        String clientId = createClient(id, "Helena Prado");

        mvc.perform(get("/internal/clientes/{id}/existe", clientId).param("orgId", id.orgId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/internal/clientes/{id}/existe", clientId).param("orgId", newId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get("/internal/clientes/{id}/existe", newId()).param("orgId", id.orgId())
                        .header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(jsonPath("$.existe").value(false));
    }
}
