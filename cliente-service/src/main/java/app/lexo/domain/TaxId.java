package app.lexo.domain;

import app.lexo.common.error.InvalidDataException;

import java.util.Objects;

public final class TaxId {

    private static final int[] CPF_WEIGHTS_1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CPF_WEIGHTS_2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_WEIGHTS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_WEIGHTS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final String text;

    private TaxId(String text) {
        this.text = text;
    }

    public static TaxId of(String text) {
        if (!isValid(text)) {
            throw new InvalidDataException("CPF ou CNPJ inválido");
        }
        return new TaxId(text);
    }

    public static TaxId optional(String text) {
        return text == null || text.isBlank() ? null : of(text);
    }

    public static TaxId stored(String text) {
        return text == null ? null : new TaxId(text);
    }

    public static boolean isValid(String text) {
        String digits = digits(text);
        if (digits.length() == 11) {
            return validCpf(digits);
        }
        if (digits.length() == 14) {
            return validCnpj(digits);
        }
        return false;
    }

    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TaxId taxId && text.equals(taxId.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return text;
    }

    private static boolean validCpf(String d) {
        if (allEqual(d)) {
            return false;
        }
        return checkDigit(d.substring(0, 9), CPF_WEIGHTS_1) == Character.getNumericValue(d.charAt(9))
                && checkDigit(d.substring(0, 10), CPF_WEIGHTS_2) == Character.getNumericValue(d.charAt(10));
    }

    private static boolean validCnpj(String d) {
        if (allEqual(d)) {
            return false;
        }
        return checkDigit(d.substring(0, 12), CNPJ_WEIGHTS_1) == Character.getNumericValue(d.charAt(12))
                && checkDigit(d.substring(0, 13), CNPJ_WEIGHTS_2) == Character.getNumericValue(d.charAt(13));
    }

    private static String digits(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }

    private static boolean allEqual(String d) {
        return d.chars().distinct().count() == 1;
    }

    private static int checkDigit(String part, int[] weights) {
        int sum = 0;
        for (int i = 0; i < part.length(); i++) {
            sum += Character.getNumericValue(part.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
