package app.lexo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validacao de CPF/CNPJ")
class DocumentValidatorTest {

    @Test
    @DisplayName("aceita CPF valido")
    void cpfValido() {
        assertTrue(DocumentValidator.isValidCpf("52998224725"));
        assertTrue(DocumentValidator.isValidCpf("390.533.447-05")); // com mascara
    }

    @Test
    @DisplayName("rejeita CPF invalido")
    void cpfInvalido() {
        assertFalse(DocumentValidator.isValidCpf("11111111111")); // todos iguais
        assertFalse(DocumentValidator.isValidCpf("12345678900")); // digito verificador errado
        assertFalse(DocumentValidator.isValidCpf("123"));         // tamanho errado
    }

    @Test
    @DisplayName("aceita CNPJ valido")
    void cnpjValido() {
        assertTrue(DocumentValidator.isValidCnpj("11222333000181"));
    }

    @Test
    @DisplayName("rejeita CNPJ invalido")
    void cnpjInvalido() {
        assertFalse(DocumentValidator.isValidCnpj("11222333000180")); // digito errado
        assertFalse(DocumentValidator.isValidCnpj("00000000000000")); // todos iguais
    }

    @Test
    @DisplayName("isValid distingue CPF (11) de CNPJ (14) pelo tamanho")
    void isValidPorTamanho() {
        assertTrue(DocumentValidator.isValid("52998224725"));
        assertTrue(DocumentValidator.isValid("11222333000181"));
        assertFalse(DocumentValidator.isValid("123456")); // nem 11 nem 14
    }
}
