package app.lexo.controller;

import app.lexo.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API de andamentos (integracao)")
class AndamentoApiIntegrationTest extends IntegracaoBase {

    private String criarAndamento(Identidade id, String processoId, String titulo, Instant data) throws Exception {
        return idDe(postar(id, "/api/processos/" + processoId + "/andamentos",
                Map.of("title", titulo, "description", "detalhe de " + titulo, "date", data.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(titulo))
                .andExpect(jsonPath("$.caseId").value(processoId)));
    }

    @Test
    @DisplayName("lista do andamento mais recente para o mais antigo")
    void listaPorDataDecrescente() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AN-001");
        Instant agora = Instant.now();
        criarAndamento(id, processoId, "Distribuicao", agora.minus(10, ChronoUnit.DAYS));
        criarAndamento(id, processoId, "Sentenca", agora);
        criarAndamento(id, processoId, "Citacao", agora.minus(5, ChronoUnit.DAYS));

        buscar(id, "/api/processos/" + processoId + "/andamentos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Sentenca"))
                .andExpect(jsonPath("$[1].title").value("Citacao"))
                .andExpect(jsonPath("$[2].title").value("Distribuicao"))
                .andExpect(jsonPath("$[0].description").value("detalhe de Sentenca"));
    }

    @Test
    @DisplayName("exclui um andamento")
    void exclui() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AN-002");
        String andamentoId = criarAndamento(id, processoId, "Juntada", Instant.now());

        apagar(id, "/api/processos/" + processoId + "/andamentos/" + andamentoId).andExpect(status().isNoContent());

        buscar(id, "/api/processos/" + processoId + "/andamentos").andExpect(jsonPath("$.length()").value(0));
        apagar(id, "/api/processos/" + processoId + "/andamentos/" + andamentoId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Andamento não encontrado"));
    }

    @Test
    @DisplayName("sem titulo ou sem data devolve 400")
    void validacao() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AN-003");

        postar(id, "/api/processos/" + processoId + "/andamentos", Map.of("title", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("processo de outro escritorio devolve 404 em listar, criar e excluir")
    void processoDeOutroEscritorio() throws Exception {
        Identidade dono = Identidade.nova();
        Identidade outro = Identidade.nova();
        String processoId = criarProcesso(dono, novoId(), "AN-004");
        String andamentoId = criarAndamento(dono, processoId, "Despacho", Instant.now());

        buscar(outro, "/api/processos/" + processoId + "/andamentos").andExpect(status().isNotFound());
        postar(outro, "/api/processos/" + processoId + "/andamentos",
                Map.of("title", "Invasao", "date", Instant.now().toString())).andExpect(status().isNotFound());
        apagar(outro, "/api/processos/" + processoId + "/andamentos/" + andamentoId).andExpect(status().isNotFound());

        buscar(dono, "/api/processos/" + processoId + "/andamentos").andExpect(jsonPath("$.length()").value(1));
    }
}
