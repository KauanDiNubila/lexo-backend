package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailAddress")
class EmailAddressTest {

    @Test
    @DisplayName("is trimmed and lower-cased so the same mailbox is always the same value")
    void normalizes() {
        assertThat(new EmailAddress("  Ana@Escritorio.COM ")).isEqualTo(new EmailAddress("ana@escritorio.com"));
        assertThat(new EmailAddress("  Ana@Escritorio.COM ").value()).isEqualTo("ana@escritorio.com");
    }

    @Test
    @DisplayName("cannot be empty")
    void rejectsEmpty() {
        assertThatThrownBy(() -> new EmailAddress(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmailAddress(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
