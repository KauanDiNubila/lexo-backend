package app.lexo.domain;

import app.lexo.common.erro.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CpfCnpj")
class CpfCnpjTest {

    @Test
    @DisplayName("aceita CPF valido, com ou sem mascara, e guarda o texto como veio")
    void cpfValido() {
        assertThat(CpfCnpj.de("52998224725").texto()).isEqualTo("52998224725");
        assertThat(CpfCnpj.de("390.533.447-05").texto()).isEqualTo("390.533.447-05");
    }

    @Test
    @DisplayName("rejeita CPF invalido, repetido ou curto")
    void cpfInvalido() {
        assertThat(CpfCnpj.valido("11111111111")).isFalse();
        assertThat(CpfCnpj.valido("12345678900")).isFalse();
        assertThat(CpfCnpj.valido("123")).isFalse();
    }

    @Test
    @DisplayName("aceita CNPJ valido")
    void cnpjValido() {
        assertThat(CpfCnpj.de("11.222.333/0001-81").texto()).isEqualTo("11.222.333/0001-81");
        assertThat(CpfCnpj.valido("11222333000181")).isTrue();
    }

    @Test
    @DisplayName("rejeita CNPJ invalido ou repetido")
    void cnpjInvalido() {
        assertThat(CpfCnpj.valido("11222333000180")).isFalse();
        assertThat(CpfCnpj.valido("00000000000000")).isFalse();
    }

    @Test
    @DisplayName("distingue CPF de CNPJ pelo numero de digitos")
    void distingueCpfDeCnpjPeloTamanho() {
        assertThat(CpfCnpj.valido("52998224725")).isTrue();
        assertThat(CpfCnpj.valido("11222333000181")).isTrue();
        assertThat(CpfCnpj.valido("123456")).isFalse();
    }

    @Test
    @DisplayName("criar com documento invalido lanca DadoInvalidoException com a mensagem de hoje")
    void criarInvalido() {
        assertThatThrownBy(() -> CpfCnpj.de("11111111111"))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("CPF ou CNPJ inválido");
    }

    @Test
    @DisplayName("opcional devolve nulo para texto ausente ou em branco")
    void opcional() {
        assertThat(CpfCnpj.opcional(null)).isNull();
        assertThat(CpfCnpj.opcional("")).isNull();
        assertThat(CpfCnpj.opcional("   ")).isNull();
        assertThat(CpfCnpj.opcional("52998224725")).isNotNull();
    }

    @Test
    @DisplayName("armazenado aceita o que ja esta salvo sem validar de novo")
    void armazenado() {
        assertThat(CpfCnpj.armazenado("documento antigo").texto()).isEqualTo("documento antigo");
        assertThat(CpfCnpj.armazenado(null)).isNull();
    }

    @Test
    @DisplayName("dois documentos com o mesmo texto sao iguais")
    void igualdade() {
        assertThat(CpfCnpj.de("52998224725")).isEqualTo(CpfCnpj.de("52998224725"));
    }
}
