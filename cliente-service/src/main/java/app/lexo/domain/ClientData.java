package app.lexo.domain;

public record ClientData(String name, TaxId taxId, String email, String phone, String notes) {

    public ClientData {
        email = blankToNull(email);
        phone = blankToNull(phone);
        notes = blankToNull(notes);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
