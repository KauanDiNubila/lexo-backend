package app.lexo.application.service;

import app.lexo.Texts;
import app.lexo.application.port.in.DraftPetitionCommand;
import app.lexo.application.port.in.PetitionResponse;
import app.lexo.application.service.Fakes.RateLimit;
import app.lexo.application.service.Fakes.Text;
import app.lexo.domain.CaseContext;
import app.lexo.domain.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static app.lexo.application.service.Fakes.ORGANIZATION;
import static app.lexo.application.service.Fakes.SETTINGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DraftPetitionService")
class DraftPetitionServiceTest {

    private final Text text = new Text();
    private final RateLimit rateLimit = new RateLimit();
    private final DraftPetitionService service = new DraftPetitionService(new UsageQuota(rateLimit, SETTINGS), text);

    @Test
    @DisplayName("sends the instruction, the request and the case context and returns the trimmed text")
    void usesTheModel() {
        text.answer = Optional.of("\n MINUTA \n");
        CaseContext context = new CaseContext("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado");

        PetitionResponse response = service.draft(
                new DraftPetitionCommand(ORGANIZATION, "Contestação trabalhista", Optional.of(context)));

        assertThat(response.text()).isEqualTo("MINUTA");
        assertThat(response.source()).isEqualTo("gemini");
        assertThat(text.prompts).containsExactly(Texts.PETITION_INTRO
                + "Pedido: Contestação trabalhista\n"
                + "\nContexto do processo:\n"
                + Texts.caseLines("0001234-56.2024.8.26.0100", "Cível", "ATIVO", "Helena Prado") + "\n");
    }

    @Test
    @DisplayName("without a case context the prompt ends after the request")
    void withoutContext() {
        text.answer = Optional.of("ok");

        service.draft(new DraftPetitionCommand(ORGANIZATION, "Contestação trabalhista", Optional.empty()));

        assertThat(text.prompts).containsExactly(Texts.PETITION_INTRO + "Pedido: Contestação trabalhista\n");
    }

    @Test
    @DisplayName("falls back to the template with the request when the model gives nothing")
    void template() {
        PetitionResponse response = service.draft(
                new DraftPetitionCommand(ORGANIZATION, "Contestação trabalhista", Optional.empty()));

        assertThat(response.source()).isEqualTo("mock");
        assertThat(response.text()).isEqualTo(Texts.petitionTemplate("Contestação trabalhista"));
    }

    @Test
    @DisplayName("a blank request is described as a legal document")
    void blankRequest() {
        PetitionResponse response = service.draft(new DraftPetitionCommand(ORGANIZATION, " ", Optional.empty()));

        assertThat(response.text()).contains("Pedido: peça jurídica");
    }

    @Test
    @DisplayName("spends the organization limit before asking the model, and refuses past it")
    void limit() {
        service.draft(new DraftPetitionCommand(ORGANIZATION, "Contestação", Optional.empty()));
        assertThat(rateLimit.calls).hasSize(1);

        rateLimit.allow = false;
        int callsBefore = text.calls();
        assertThatThrownBy(() -> service.draft(new DraftPetitionCommand(ORGANIZATION, "Contestação", Optional.empty())))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(text.calls()).isEqualTo(callsBefore);
    }
}
