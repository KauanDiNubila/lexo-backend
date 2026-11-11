package app.lexo.domain;

import app.lexo.common.error.InvalidDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaxId")
class TaxIdTest {

    @Test
    @DisplayName("accepts a valid CPF, with or without mask, and keeps the text as it came")
    void validCpf() {
        assertThat(TaxId.of("52998224725").text()).isEqualTo("52998224725");
        assertThat(TaxId.of("390.533.447-05").text()).isEqualTo("390.533.447-05");
    }

    @Test
    @DisplayName("rejects an invalid, repeated or short CPF")
    void invalidCpf() {
        assertThat(TaxId.isValid("11111111111")).isFalse();
        assertThat(TaxId.isValid("12345678900")).isFalse();
        assertThat(TaxId.isValid("123")).isFalse();
    }

    @Test
    @DisplayName("accepts a valid CNPJ")
    void validCnpj() {
        assertThat(TaxId.of("11.222.333/0001-81").text()).isEqualTo("11.222.333/0001-81");
        assertThat(TaxId.isValid("11222333000181")).isTrue();
    }

    @Test
    @DisplayName("rejects an invalid or repeated CNPJ")
    void invalidCnpj() {
        assertThat(TaxId.isValid("11222333000180")).isFalse();
        assertThat(TaxId.isValid("00000000000000")).isFalse();
    }

    @Test
    @DisplayName("tells CPF from CNPJ by the number of digits")
    void distinguishesCpfFromCnpjByLength() {
        assertThat(TaxId.isValid("52998224725")).isTrue();
        assertThat(TaxId.isValid("11222333000181")).isTrue();
        assertThat(TaxId.isValid("123456")).isFalse();
    }

    @Test
    @DisplayName("creating with an invalid document throws InvalidDataException with today's message")
    void createInvalid() {
        assertThatThrownBy(() -> TaxId.of("11111111111"))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("CPF ou CNPJ inválido");
    }

    @Test
    @DisplayName("optional returns null for missing or blank text")
    void optional() {
        assertThat(TaxId.optional(null)).isNull();
        assertThat(TaxId.optional("")).isNull();
        assertThat(TaxId.optional("   ")).isNull();
        assertThat(TaxId.optional("52998224725")).isNotNull();
    }

    @Test
    @DisplayName("stored accepts what is already saved without validating again")
    void stored() {
        assertThat(TaxId.stored("documento antigo").text()).isEqualTo("documento antigo");
        assertThat(TaxId.stored(null)).isNull();
    }

    @Test
    @DisplayName("two documents with the same text are equal")
    void equality() {
        assertThat(TaxId.of("52998224725")).isEqualTo(TaxId.of("52998224725"));
    }
}
