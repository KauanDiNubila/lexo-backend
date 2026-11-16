package app.lexo.application.service;

import app.lexo.application.port.in.AuthenticateUseCase;
import app.lexo.application.port.out.SessionCutoffPort;
import app.lexo.application.port.out.TokenVerifierPort;
import app.lexo.domain.PublicPaths;
import app.lexo.domain.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class AuthenticateService implements AuthenticateUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateService.class);
    private static final String BEARER = "Bearer ";

    private final TokenVerifierPort tokens;
    private final SessionCutoffPort cutoffs;

    AuthenticateService(TokenVerifierPort tokens, SessionCutoffPort cutoffs) {
        this.tokens = tokens;
        this.cutoffs = cutoffs;
    }

    @Override
    public Mono<Access> authenticate(String path, String authorization) {
        if (PublicPaths.isPublic(path)) {
            return Mono.just(Access.PUBLIC);
        }
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return Mono.just(Access.DENIED);
        }
        return tokens.verify(authorization.substring(BEARER.length()))
                .map(this::checkCutoff)
                .orElseGet(() -> Mono.just(Access.DENIED));
    }

    private Mono<Access> checkCutoff(Session session) {
        return cutoffs.cutoffOf(session.identity().userId())
                .map(session::isEndedBy)
                .defaultIfEmpty(false)
                .onErrorResume(e -> {
                    log.warn("[auth] could not check the session cutoff, letting the request through", e);
                    return Mono.just(false);
                })
                .map(ended -> ended ? Access.DENIED : new Access.Granted(session.identity()));
    }
}
