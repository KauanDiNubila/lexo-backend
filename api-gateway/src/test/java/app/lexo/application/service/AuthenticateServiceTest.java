package app.lexo.application.service;

import app.lexo.application.port.in.AuthenticateUseCase.Access;
import app.lexo.application.port.out.SessionCutoffPort;
import app.lexo.application.port.out.TokenVerifierPort;
import app.lexo.domain.Identity;
import app.lexo.domain.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authenticate use case")
class AuthenticateServiceTest {

    private static final Identity ANA = new Identity("u-1", "org-1", "ADMIN", "Ana", "ana@x.com");

    private Optional<Session> verified = Optional.of(new Session(ANA, OptionalLong.of(1_000)));
    private Mono<Long> cutoff = Mono.empty();
    private final List<String> verifiedTokens = new ArrayList<>();
    private final List<String> cutoffLookups = new ArrayList<>();

    private final TokenVerifierPort tokens = token -> {
        verifiedTokens.add(token);
        return verified;
    };
    private final SessionCutoffPort cutoffs = userId -> {
        cutoffLookups.add(userId);
        return cutoff;
    };
    private final AuthenticateService service = new AuthenticateService(tokens, cutoffs);

    private Access authenticate(String path, String authorization) {
        return service.authenticate(path, authorization).block();
    }

    @Test
    @DisplayName("a public path needs no token and never asks the token or the cutoff")
    void publicPath() {
        assertThat(authenticate("/api/auth/login", null)).isInstanceOf(Access.Public.class);

        assertThat(verifiedTokens).isEmpty();
        assertThat(cutoffLookups).isEmpty();
    }

    @Test
    @DisplayName("a private path without Authorization is denied")
    void noAuthorization() {
        assertThat(authenticate("/api/clientes", null)).isInstanceOf(Access.Denied.class);
        assertThat(verifiedTokens).isEmpty();
    }

    @Test
    @DisplayName("Authorization without the Bearer prefix is denied")
    void noBearerPrefix() {
        assertThat(authenticate("/api/clientes", "abc.def.ghi")).isInstanceOf(Access.Denied.class);
        assertThat(verifiedTokens).isEmpty();
    }

    @Test
    @DisplayName("a token that does not verify is denied")
    void invalidToken() {
        verified = Optional.empty();

        assertThat(authenticate("/api/clientes", "Bearer ruim")).isInstanceOf(Access.Denied.class);
        assertThat(verifiedTokens).containsExactly("ruim");
    }

    @Test
    @DisplayName("a valid token without a cutoff is granted with the identity from the token")
    void granted() {
        assertThat(authenticate("/api/clientes", "Bearer bom")).isEqualTo(new Access.Granted(ANA));
        assertThat(cutoffLookups).containsExactly("u-1");
    }

    @Test
    @DisplayName("a token issued at or before the cutoff is denied")
    void issuedBeforeTheCutoff() {
        cutoff = Mono.just(1_000L);

        assertThat(authenticate("/api/clientes", "Bearer bom")).isInstanceOf(Access.Denied.class);
    }

    @Test
    @DisplayName("a token issued after the cutoff is granted")
    void issuedAfterTheCutoff() {
        cutoff = Mono.just(999L);

        assertThat(authenticate("/api/clientes", "Bearer bom")).isInstanceOf(Access.Granted.class);
    }

    @Test
    @DisplayName("a token without an issue time is denied when the user has a cutoff")
    void noIssueTimeUnderCutoff() {
        verified = Optional.of(new Session(ANA, OptionalLong.empty()));
        cutoff = Mono.just(1L);

        assertThat(authenticate("/api/clientes", "Bearer bom")).isInstanceOf(Access.Denied.class);
    }

    @Test
    @DisplayName("when the cutoff cannot be read the request is let through")
    void failsOpen() {
        cutoff = Mono.error(new IllegalStateException("redis fora do ar"));

        assertThat(authenticate("/api/clientes", "Bearer bom")).isInstanceOf(Access.Granted.class);
    }
}
