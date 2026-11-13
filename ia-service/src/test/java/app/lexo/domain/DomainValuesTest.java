package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Domain values")
class DomainValuesTest {

    @Test
    @DisplayName("the source codes are the ones the API has always returned")
    void sourceCodes() {
        assertThat(Source.MODEL.code()).isEqualTo("gemini");
        assertThat(Source.FALLBACK.code()).isEqualTo("mock");
    }

    @Test
    @DisplayName("an organization id cannot be null or blank")
    void organizationId() {
        assertThat(new OrganizationId("org-1").value()).isEqualTo("org-1");
        assertThatThrownBy(() -> new OrganizationId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrganizationId(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the rate limit exception carries the user message")
    void rateLimitMessage() {
        assertThat(new RateLimitExceededException())
                .hasMessage("Muitas requisições de IA. Tente novamente em instantes.");
    }
}
