package app.lexo.domain;

public record InvoiceId(String value) {

    public InvoiceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("InvoiceId is empty");
        }
    }
}
