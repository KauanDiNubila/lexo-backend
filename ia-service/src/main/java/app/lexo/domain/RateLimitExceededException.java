package app.lexo.domain;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Muitas requisições de IA. Tente novamente em instantes.");
    }
}
