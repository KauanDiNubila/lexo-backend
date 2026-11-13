package app.lexo.domain;

public enum Source {

    MODEL("gemini"),
    FALLBACK("mock");

    private final String code;

    Source(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
