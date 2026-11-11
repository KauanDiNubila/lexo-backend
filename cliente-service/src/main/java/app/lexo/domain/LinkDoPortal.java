package app.lexo.domain;

import java.time.Instant;

public record LinkDoPortal(String token, Instant expiraEm) {

    public LinkDoPortal {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token do portal vazio");
        }
    }

    public boolean ativoEm(Instant agora) {
        return expiraEm != null && expiraEm.isAfter(agora);
    }
}
