package app.lexo.application.service;

import app.lexo.application.port.out.AccountEvent;
import app.lexo.application.port.out.InviteEmail;
import app.lexo.application.port.out.IssueAccessTokenPort;
import app.lexo.application.port.out.LoginAttemptsPort;
import app.lexo.application.port.out.PasswordHasher;
import app.lexo.application.port.out.PublishEventPort;
import app.lexo.application.port.out.RevokeAccessTokensPort;
import app.lexo.application.port.out.SecretCipherPort;
import app.lexo.application.port.out.SendInviteEmailPort;
import app.lexo.application.port.out.TotpPort;
import app.lexo.common.error.ServiceUnavailableException;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.User;
import app.lexo.domain.UserId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Fakes {

    static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private Fakes() {
    }

    static class Hasher implements PasswordHasher {

        int wastedTime;

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash(rawPassword).equals(hash);
        }

        @Override
        public void consumeTime(String rawPassword) {
            wastedTime++;
        }
    }

    static class AccessTokens implements IssueAccessTokenPort {

        @Override
        public String issue(User user) {
            return "jwt-for-" + user.id().value();
        }
    }

    static class Cipher implements SecretCipherPort {

        @Override
        public String encrypt(String plain) {
            return "enc:" + plain;
        }

        @Override
        public String decrypt(String stored) {
            return stored.substring("enc:".length());
        }
    }

    static class Totp implements TotpPort {

        static final String VALID_CODE = "123456";

        int generated;

        @Override
        public String generateSecret() {
            return "SECRET" + (++generated);
        }

        @Override
        public boolean verify(String secret, String code) {
            return VALID_CODE.equals(code);
        }

        @Override
        public String otpauthUri(String secret, EmailAddress account) {
            return "otpauth://" + account.value() + "?secret=" + secret;
        }
    }

    static class Attempts implements LoginAttemptsPort {

        final Map<String, Integer> counters = new HashMap<>();
        final List<String> registered = new ArrayList<>();
        final List<String> reset = new ArrayList<>();
        final Map<String, Duration> windows = new HashMap<>();

        @Override
        public boolean register(String key, int max, Duration window) {
            registered.add(key);
            windows.put(key, window);
            int count = counters.merge(key, 1, Integer::sum);
            return count <= max;
        }

        @Override
        public void reset(String key) {
            reset.add(key);
            counters.remove(key);
        }
    }

    static class Events implements PublishEventPort {

        final List<AccountEvent> published = new ArrayList<>();

        @Override
        public void publish(AccountEvent event) {
            published.add(event);
        }
    }

    static class AccessCutoffs implements RevokeAccessTokensPort {

        final Map<UserId, Instant> revoked = new LinkedHashMap<>();
        boolean failing;

        @Override
        public void revokeIssuedUpTo(UserId userId, Instant now) {
            if (failing) {
                throw new ServiceUnavailableException("redis down");
            }
            revoked.put(userId, now);
        }
    }

    static class FailingInviteEmails implements SendInviteEmailPort {

        @Override
        public boolean send(InviteEmail email) {
            return false;
        }
    }

    static class InviteEmails implements SendInviteEmailPort {

        final List<InviteEmail> sent = new ArrayList<>();

        @Override
        public boolean send(InviteEmail email) {
            sent.add(email);
            return true;
        }
    }
}
