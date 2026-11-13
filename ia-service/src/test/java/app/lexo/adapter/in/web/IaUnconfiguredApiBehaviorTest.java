package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import app.lexo.Texts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "lexo.ia.gemini.api-key=")
@DisplayName("AI API without a Gemini key: behavior pinned before the hexagonal migration")
class IaUnconfiguredApiBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("without a key no endpoint calls Gemini and all answer with the automatic text")
    void neverCallsGemini() throws Exception {
        GEMINI.answers("nao deveria ser usado");
        Identity id = Identity.create();

        postAs(id, "/api/ia/resumir-processo", Map.of("numero", "123", "cliente", "Helena Prado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.resumo").value(
                        "O processo 123 do cliente Helena Prado encontra-se em andamento. "
                                + "Não há prazos cadastrados no momento." + Texts.SUMMARY_MOCK_TAIL));
        postAs(id, "/api/ia/chat", Map.of("mensagens", List.of(Map.of("papel", "user", "texto", "Oi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.resposta").value(Texts.CHAT_UNAVAILABLE));
        postAs(id, "/api/ia/rascunhar-peticao", Map.of("pedido", "Contestação"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fonte").value("mock"))
                .andExpect(jsonPath("$.texto").value(Texts.petitionTemplate("Contestação")));

        assertThat(GEMINI.requests()).isEmpty();
    }

    @Test
    @DisplayName("without a key the limit is still spent")
    void stillSpendsTheLimit() throws Exception {
        redisCounts(4L);

        postAs(Identity.create(), "/api/ia/resumir-processo", Map.of())
                .andExpect(status().isTooManyRequests());
    }
}
