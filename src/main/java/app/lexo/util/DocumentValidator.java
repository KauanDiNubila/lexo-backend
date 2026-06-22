package app.lexo.util;

/** Validacao de CPF/CNPJ, portada de lib/document.ts. */
public final class DocumentValidator {

    private DocumentValidator() {
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static boolean allSame(String d) {
        for (int i = 1; i < d.length(); i++) {
            if (d.charAt(i) != d.charAt(0)) return false;
        }
        return true;
    }

    private static int checkDigit(String slice, int[] weights) {
        int sum = 0;
        for (int i = 0; i < slice.length(); i++) {
            sum += Character.getNumericValue(slice.charAt(i)) * weights[i];
        }
        int rem = sum % 11;
        return rem < 2 ? 0 : 11 - rem;
    }

    public static boolean isValidCpf(String value) {
        String d = digits(value);
        if (d.length() != 11 || allSame(d)) return false;

        int first = checkDigit(d.substring(0, 9), new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        if (first != Character.getNumericValue(d.charAt(9))) return false;

        int second = checkDigit(d.substring(0, 10), new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        return second == Character.getNumericValue(d.charAt(10));
    }

    public static boolean isValidCnpj(String value) {
        String d = digits(value);
        if (d.length() != 14 || allSame(d)) return false;

        int first = checkDigit(d.substring(0, 12), new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        if (first != Character.getNumericValue(d.charAt(12))) return false;

        int second = checkDigit(d.substring(0, 13), new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return second == Character.getNumericValue(d.charAt(13));
    }

    /** Aceita CPF (11 digitos) ou CNPJ (14 digitos). */
    public static boolean isValid(String value) {
        String d = digits(value);
        if (d.length() == 11) return isValidCpf(d);
        if (d.length() == 14) return isValidCnpj(d);
        return false;
    }
}
