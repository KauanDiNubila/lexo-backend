package app.lexo.controller;

import app.lexo.IntegrationTestBase;
import app.lexo.common.reference.ExistsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Cases API (integration)")
class CaseApiBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("creates a case, lists it and publishes PROCESSO_CRIADO")
    void createsAndListsPublishingEvent() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();

        postAs(id, "/api/processos", caseData(clientId, "0001-A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value("0001-A"))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        getAs(id, "/api/processos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(events).publish(argThat(e -> "PROCESSO_CRIADO".equals(e.type())
                && id.orgId().equals(e.organizationId())
                && "PROCESSO".equals(e.entityType())));
    }

    @Test
    @DisplayName("an unknown client returns 404 and saves nothing")
    void unknownClient() throws Exception {
        Identity id = Identity.create();
        String clientId = newId();
        when(clientService.clienteExiste(eq(clientId), anyString())).thenReturn(new ExistsResponse(false));

        postAs(id, "/api/processos", caseData(clientId, "0002-A"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado"))
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));

        getAs(id, "/api/processos").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("an unknown responsible returns 404")
    void unknownResponsible() throws Exception {
        Identity id = Identity.create();
        String responsibleId = newId();
        when(authService.usuarioExiste(eq(responsibleId), anyString())).thenReturn(new ExistsResponse(false));
        Map<String, Object> data = new HashMap<>(caseData(newId(), "0003-A"));
        data.put("responsavelId", responsibleId);

        postAs(id, "/api/processos", data)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Responsável não encontrado"));
    }

    @Test
    @DisplayName("the client service being down returns 503")
    void clientServiceDown() throws Exception {
        Identity id = Identity.create();
        when(clientService.clienteExiste(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        postAs(id, "/api/processos", caseData(newId(), "0004-A"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("Serviço de clientes temporariamente indisponível. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("a missing client and number return 400 with both messages")
    void bodyValidation() throws Exception {
        Identity id = Identity.create();

        postAs(id, "/api/processos", Map.of("clientId", "", "number", "", "status", "ATIVO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error", containsString("Selecione um cliente")))
                .andExpect(jsonPath("$.error", containsString("Número do processo é obrigatório")));
    }

    @Test
    @DisplayName("a repeated number in the same organization returns 500 today; in another organization it is allowed")
    void repeatedNumber() throws Exception {
        Identity a = Identity.create();
        Identity b = Identity.create();
        createCase(a, newId(), "0005-A");

        postAs(a, "/api/processos", caseData(newId(), "0005-A"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERRO_INTERNO"));

        postAs(b, "/api/processos", caseData(newId(), "0005-A")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("updates and finds a case; another organization gets 404")
    void updatesAndFinds() throws Exception {
        Identity id = Identity.create();
        Identity other = Identity.create();
        String caseId = createCase(id, newId(), "0006-A");
        Map<String, Object> changes = Map.of("clientId", newId(), "number", "0006-B", "area", "Trabalhista",
                "status", "SUSPENSO", "description", "changed");

        putAs(id, "/api/processos/" + caseId, changes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("0006-B"))
                .andExpect(jsonPath("$.status").value("SUSPENSO"));

        getAs(id, "/api/processos/" + caseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area").value("Trabalhista"))
                .andExpect(jsonPath("$.description").value("changed"));
        getAs(other, "/api/processos/" + caseId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Processo não encontrado"));
        verify(events).publish(argThat(e -> "PROCESSO_ATUALIZADO".equals(e.type())
                && caseId.equals(e.entityId())));
    }

    @Test
    @DisplayName("deleting requires ADVOGADO; SECRETARIA gets 403")
    void deleteByRole() throws Exception {
        Identity admin = Identity.create();
        String caseId = createCase(admin, newId(), "0007-A");

        deleteAs(admin.withRole("SECRETARIA"), "/api/processos/" + caseId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        getAs(admin, "/api/processos/" + caseId).andExpect(status().isOk());

        deleteAs(admin.withRole("ADVOGADO"), "/api/processos/" + caseId).andExpect(status().isNoContent());
        getAs(admin, "/api/processos/" + caseId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting removes deadlines and movements and publishes PROCESSO_EXCLUIDO")
    void cascadeDelete() throws Exception {
        Identity id = Identity.create();
        String caseId = createCase(id, newId(), "0008-A");
        createDeadline(id, caseId, "PRAZO", Duration.ofDays(10));
        createMovement(id, caseId, "Petition filed", Instant.now());

        deleteAs(id, "/api/processos/" + caseId).andExpect(status().isNoContent());

        getAs(id, "/api/agenda?caseId=" + caseId).andExpect(jsonPath("$.totalElements").value(0));
        getAs(id, "/api/processos/" + caseId + "/andamentos").andExpect(status().isNotFound());
        verify(events).publish(argThat(e -> "PROCESSO_EXCLUIDO".equals(e.type())
                && caseId.equals(e.entityId())));
    }

    @Test
    @DisplayName("listing orders newest first, pages, and filters by status and text")
    void listing() throws Exception {
        Identity id = Identity.create();
        createCase(id, newId(), "L-001");
        postAs(id, "/api/processos", Map.of("clientId", newId(), "number", "L-002", "area", "Trabalhista",
                "status", "SUSPENSO")).andExpect(status().isCreated());
        createCase(id, newId(), "L-003");

        getAs(id, "/api/processos")
                .andExpect(jsonPath("$.content[0].number").value("L-003"))
                .andExpect(jsonPath("$.content[2].number").value("L-001"));
        getAs(id, "/api/processos?size=2&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        getAs(id, "/api/processos?status=SUSPENSO")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("L-002"));
        getAs(id, "/api/processos?q=TRABALHISTA").andExpect(jsonPath("$.totalElements").value(1));
        getAs(id, "/api/processos?q=l-003").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("options and summary reflect the organization's cases")
    void optionsAndSummary() throws Exception {
        Identity id = Identity.create();
        String first = createCase(id, newId(), "O-001");
        postAs(id, "/api/processos", Map.of("clientId", newId(), "number", "O-002", "status", "ENCERRADO"))
                .andExpect(status().isCreated());

        getAs(id, "/api/processos/opcoes")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("O-002"))
                .andExpect(jsonPath("$[1].id").value(first));
        getAs(id, "/api/processos/resumo")
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.ativos").value(1))
                .andExpect(jsonPath("$.porStatus.ATIVO").value(1))
                .andExpect(jsonPath("$.porStatus.ENCERRADO").value(1))
                .andExpect(jsonPath("$.porStatus.SUSPENSO").value(0))
                .andExpect(jsonPath("$.porStatus.ARQUIVADO").value(0));
    }
}
