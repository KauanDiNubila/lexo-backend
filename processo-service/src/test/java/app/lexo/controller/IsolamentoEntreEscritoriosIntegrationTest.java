package app.lexo.controller;

import app.lexo.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Isolamento entre escritorios no processo-service (integracao)")
class IsolamentoEntreEscritoriosIntegrationTest extends IntegracaoBase {

    @Test
    @DisplayName("um escritorio nao enxerga, altera nem apaga os dados de outro")
    void escritorioNaoAlcancaDadosDeOutro() throws Exception {
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();
        String clienteId = novoId();
        String processoId = criarProcesso(a, clienteId, "IS-001");
        String prazoId = criarPrazo(a, processoId, "PRAZO", Duration.ofDays(5));
        String andamentoId = idDe(postar(a, "/api/processos/" + processoId + "/andamentos",
                Map.of("title", "Despacho", "date", Instant.now().toString())).andExpect(status().isCreated()));

        buscar(b, "/api/processos").andExpect(jsonPath("$.totalElements").value(0));
        buscar(b, "/api/processos/opcoes").andExpect(jsonPath("$.length()").value(0));
        buscar(b, "/api/processos/resumo").andExpect(jsonPath("$.total").value(0));
        buscar(b, "/api/processos/" + processoId).andExpect(status().isNotFound());
        colocar(b, "/api/processos/" + processoId, dadosDoProcesso(clienteId, "IS-HACK")).andExpect(status().isNotFound());
        apagar(b, "/api/processos/" + processoId).andExpect(status().isNotFound());

        buscar(b, "/api/agenda").andExpect(jsonPath("$.totalElements").value(0));
        buscar(b, "/api/agenda?caseId=" + processoId).andExpect(jsonPath("$.totalElements").value(0));
        buscar(b, "/api/agenda/resumo").andExpect(jsonPath("$.proximos.length()").value(0));
        alternarStatus(b, prazoId, true).andExpect(status().isNotFound());
        colocar(b, "/api/agenda/" + prazoId, Map.of("caseId", processoId, "title", "x", "type", "PRAZO",
                "date", Instant.now().toString())).andExpect(status().isNotFound());
        apagar(b, "/api/agenda/" + prazoId).andExpect(status().isNoContent());

        buscar(b, "/api/processos/" + processoId + "/andamentos").andExpect(status().isNotFound());
        apagar(b, "/api/processos/" + processoId + "/andamentos/" + andamentoId).andExpect(status().isNotFound());

        buscar(a, "/api/processos/" + processoId).andExpect(jsonPath("$.number").value("IS-001"));
        buscar(a, "/api/agenda").andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
        buscar(a, "/api/processos/" + processoId + "/andamentos").andExpect(jsonPath("$.length()").value(1));
    }
}
