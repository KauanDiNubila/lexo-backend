package app.lexo.controller;

import app.lexo.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API de agenda (integracao)")
class AgendaApiIntegrationTest extends IntegracaoBase {

    private Map<String, Object> dadosDoPrazo(String processoId, String titulo, String tipo, Duration quando) {
        return Map.of("caseId", processoId, "title", titulo, "type", tipo, "date", Instant.now().plus(quando).toString());
    }

    @Test
    @DisplayName("cria, atualiza, conclui, reabre e exclui um prazo")
    void ciclo() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AG-001");

        String prazoId = idDe(postar(id, "/api/agenda", dadosDoPrazo(processoId, "Contestar", "PRAZO", Duration.ofDays(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.caseId").value(processoId)));

        colocar(id, "/api/agenda/" + prazoId, dadosDoPrazo(processoId, "Contestar com documentos", "AUDIENCIA", Duration.ofDays(20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Contestar com documentos"))
                .andExpect(jsonPath("$.type").value("AUDIENCIA"));

        alternarStatus(id, prazoId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.risk").doesNotExist());

        alternarStatus(id, prazoId, false)
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.risk").exists());

        apagar(id, "/api/agenda/" + prazoId).andExpect(status().isNoContent());
        buscar(id, "/api/agenda").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("prazo de processo de outro escritorio devolve 404")
    void processoDeOutroEscritorio() throws Exception {
        Identidade dono = Identidade.nova();
        Identidade outro = Identidade.nova();
        String processoId = criarProcesso(dono, novoId(), "AG-002");

        postar(outro, "/api/agenda", dadosDoPrazo(processoId, "Invasao", "PRAZO", Duration.ofDays(3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Processo não encontrado"));
    }

    @Test
    @DisplayName("prazo sem titulo ou sem data devolve 400")
    void validacao() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AG-003");

        postar(id, "/api/agenda", Map.of("caseId", processoId, "title", "", "type", "PRAZO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("lista todos os prazos do escritorio, ou so os de um processo, do mais proximo ao mais distante")
    void listagem() throws Exception {
        Identidade id = Identidade.nova();
        String primeiro = criarProcesso(id, novoId(), "AG-004");
        String segundo = criarProcesso(id, novoId(), "AG-005");
        criarPrazo(id, primeiro, "PRAZO", Duration.ofDays(30));
        criarPrazo(id, primeiro, "PRAZO", Duration.ofDays(5));
        criarPrazo(id, segundo, "REUNIAO", Duration.ofDays(9));

        buscar(id, "/api/agenda")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].caseId").value(primeiro))
                .andExpect(jsonPath("$.content[2].caseId").value(primeiro));
        buscar(id, "/api/agenda?caseId=" + segundo)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("REUNIAO"));
    }

    @Test
    @DisplayName("o risco depende da distancia da data e do tipo do prazo")
    void risco() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AG-006");
        String criticoPorHoras = criarPrazo(id, processoId, "PRAZO", Duration.ofHours(12));
        String urgente = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(2));
        String altoPorFator = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(10));
        String medioSemFator = criarPrazo(id, processoId, "OUTRO", Duration.ofDays(10));
        String baixo = criarPrazo(id, processoId, "AUDIENCIA", Duration.ofDays(30));
        String atrasado = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(-3));

        buscar(id, "/api/agenda?size=50").andExpect(status().isOk());
        riscoDe(id, criticoPorHoras, "CRITICO");
        riscoDe(id, urgente, "URGENTE");
        riscoDe(id, altoPorFator, "ALTO");
        riscoDe(id, medioSemFator, "MEDIO");
        riscoDe(id, baixo, "BAIXO");
        riscoDe(id, atrasado, "CRITICO");
    }

    private void riscoDe(Identidade id, String prazoId, String esperado) throws Exception {
        var conteudo = corpoDe(buscar(id, "/api/agenda?size=50")).get("content");
        for (var prazo : conteudo) {
            if (prazoId.equals(prazo.get("id").asText())) {
                assertThat(prazo.get("risk").asText()).isEqualTo(esperado);
                return;
            }
        }
        fail("prazo %s nao encontrado na agenda", prazoId);
    }

    @Test
    @DisplayName("resumo conta urgentes (7 dias), vencidos e devolve os 5 proximos pendentes")
    void resumo() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "AG-007");
        String vencido = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(-3));
        String proximo = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(2));
        criarPrazo(id, processoId, "PRAZO", Duration.ofDays(5));
        criarPrazo(id, processoId, "PRAZO", Duration.ofDays(20));
        String concluido = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(1));
        alternarStatus(id, concluido, true);

        buscar(id, "/api/agenda/resumo")
                .andExpect(jsonPath("$.urgentes").value(2))
                .andExpect(jsonPath("$.vencidos").value(1))
                .andExpect(jsonPath("$.proximos.length()").value(4))
                .andExpect(jsonPath("$.proximos[0].id").value(vencido))
                .andExpect(jsonPath("$.proximos[1].id").value(proximo));
    }
}
