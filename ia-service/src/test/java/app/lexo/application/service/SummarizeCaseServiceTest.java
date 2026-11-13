package app.lexo.application.service;

import app.lexo.Texts;
import app.lexo.application.port.in.SummarizeCaseCommand;
import app.lexo.application.port.in.SummaryResponse;
import app.lexo.application.service.Fakes.RateLimit;
import app.lexo.application.service.Fakes.Text;
import app.lexo.domain.CaseContext;
import app.lexo.domain.Deadline;
import app.lexo.domain.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static app.lexo.application.service.Fakes.ORGANIZATION;
import static app.lexo.application.service.Fakes.SETTINGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SummarizeCaseService")
class SummarizeCaseServiceTest {

    private final Text text = new Text();
    private final RateLimit rateLimit = new RateLimit();
    private final SummarizeCaseService service = new SummarizeCaseService(new UsageQuota(rateLimit, SETTINGS), text);

    private final CaseContext fullContext = new CaseContext("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado");
    private final List<Deadline> deadlines = List.of(
            new Deadline("Contestação", "2026-12-01", "PENDENTE"),
            new Deadline("Audiência", "2026-12-10", "PENDENTE"));

    @Test
    @DisplayName("sends the case and every deadline in the prompt and returns the trimmed model answer")
    void usesTheModel() {
        text.answer = Optional.of("  O processo segue.  \n");

        SummaryResponse response = service.summarize(new SummarizeCaseCommand(ORGANIZATION, fullContext, deadlines));

        assertThat(response.summary()).isEqualTo("O processo segue.");
        assertThat(response.source()).isEqualTo("gemini");
        assertThat(text.prompts).containsExactly(Texts.SUMMARY_INTRO
                + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado") + "\n"
                + "Prazos:\n"
                + "  - Contestação (2026-12-01, PENDENTE)\n"
                + "  - Audiência (2026-12-10, PENDENTE)\n");
    }

    @Test
    @DisplayName("uses the default wording for missing fields and says there are no deadlines")
    void defaultWording() {
        text.answer = Optional.of("ok");

        service.summarize(new SummarizeCaseCommand(ORGANIZATION, new CaseContext(null, " ", null, null), null));

        assertThat(text.prompts).containsExactly(Texts.SUMMARY_INTRO
                + Texts.caseLines("não informado", "não informada", "não informado", "não informado") + "\n"
                + "Prazos: nenhum cadastrado.\n");
    }

    @Test
    @DisplayName("falls back to the automatic summary when the model gives nothing")
    void automaticSummary() {
        SummaryResponse response = service.summarize(new SummarizeCaseCommand(ORGANIZATION, fullContext, deadlines));

        assertThat(response.source()).isEqualTo("mock");
        assertThat(response.summary()).isEqualTo(
                "O processo 0001234-56.2024.8.26.0100 (Cível) do cliente Helena Prado encontra-se ativo. "
                        + "Há 2 prazo(s) em aberto; o próximo é \"Contestação\" em 2026-12-01."
                        + Texts.SUMMARY_MOCK_TAIL);
    }

    @Test
    @DisplayName("the automatic summary without data names no number, no area and no deadlines")
    void automaticSummaryWithoutData() {
        SummaryResponse response = service.summarize(
                new SummarizeCaseCommand(ORGANIZATION, new CaseContext(null, null, null, null), List.of()));

        assertThat(response.summary()).isEqualTo(
                "O processo sem número do cliente não informado encontra-se em andamento. "
                        + "Não há prazos cadastrados no momento." + Texts.SUMMARY_MOCK_TAIL);
    }

    @Test
    @DisplayName("the automatic summary omits the date of a deadline that has none")
    void automaticSummaryDeadlineWithoutDate() {
        SummaryResponse response = service.summarize(new SummarizeCaseCommand(ORGANIZATION,
                new CaseContext("123", null, null, null), List.of(new Deadline(null, null, "PENDENTE"))));

        assertThat(response.summary()).contains("Há 1 prazo(s) em aberto; o próximo é \"prazo\".");
    }

    @Test
    @DisplayName("spends the organization limit before asking the model")
    void spendsTheLimitFirst() {
        service.summarize(new SummarizeCaseCommand(ORGANIZATION, fullContext, deadlines));

        assertThat(rateLimit.calls).hasSize(1);
        assertThat(rateLimit.calls.get(0).organizationId()).isEqualTo(ORGANIZATION);
    }

    @Test
    @DisplayName("past the limit it refuses without asking the model")
    void refusesPastTheLimit() {
        rateLimit.allow = false;

        assertThatThrownBy(() -> service.summarize(new SummarizeCaseCommand(ORGANIZATION, fullContext, deadlines)))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(text.calls()).isZero();
    }
}
