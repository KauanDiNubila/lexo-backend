package app.lexo.domain;

import app.lexo.common.erro.DadoInvalidoException;

import java.util.Objects;

public final class CpfCnpj {

    private static final int[] PESOS_CPF_1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CPF_2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final String texto;

    private CpfCnpj(String texto) {
        this.texto = texto;
    }

    public static CpfCnpj de(String texto) {
        if (!valido(texto)) {
            throw new DadoInvalidoException("CPF ou CNPJ inválido");
        }
        return new CpfCnpj(texto);
    }

    public static CpfCnpj opcional(String texto) {
        return texto == null || texto.isBlank() ? null : de(texto);
    }

    public static CpfCnpj armazenado(String texto) {
        return texto == null ? null : new CpfCnpj(texto);
    }

    public static boolean valido(String texto) {
        String digitos = digitos(texto);
        if (digitos.length() == 11) {
            return cpfValido(digitos);
        }
        if (digitos.length() == 14) {
            return cnpjValido(digitos);
        }
        return false;
    }

    public String texto() {
        return texto;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof CpfCnpj cpfCnpj && texto.equals(cpfCnpj.texto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texto);
    }

    @Override
    public String toString() {
        return texto;
    }

    private static boolean cpfValido(String d) {
        if (todosIguais(d)) {
            return false;
        }
        return digitoVerificador(d.substring(0, 9), PESOS_CPF_1) == Character.getNumericValue(d.charAt(9))
                && digitoVerificador(d.substring(0, 10), PESOS_CPF_2) == Character.getNumericValue(d.charAt(10));
    }

    private static boolean cnpjValido(String d) {
        if (todosIguais(d)) {
            return false;
        }
        return digitoVerificador(d.substring(0, 12), PESOS_CNPJ_1) == Character.getNumericValue(d.charAt(12))
                && digitoVerificador(d.substring(0, 13), PESOS_CNPJ_2) == Character.getNumericValue(d.charAt(13));
    }

    private static String digitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static boolean todosIguais(String d) {
        return d.chars().distinct().count() == 1;
    }

    private static int digitoVerificador(String parte, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < parte.length(); i++) {
            soma += Character.getNumericValue(parte.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
