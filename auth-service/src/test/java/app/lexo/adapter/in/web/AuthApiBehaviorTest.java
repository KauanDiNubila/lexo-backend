package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth API: register, login, refresh and logout")
class AuthApiBehaviorTest extends IntegrationTestBase {

    private Map<String, Object> registration(String email, String password, String confirmation) {
        return Map.of("organizationName", "Escritorio Silva", "name", "Ana Silva", "email", email,
                "password", password, "confirmPassword", confirmation);
    }

    private String sha256(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private void limitReachedFor(Long count) {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenReturn(count);
    }

    @Test
    @DisplayName("register creates the organization on a 30 day trial and an admin with a lower-cased email")
    void registerCreatesOrganizationAndAdmin() throws Exception {
        String email = uniqueEmail();

        ResultActions response = postPublic("/api/auth/register", registration(email.toUpperCase(),
                PASSWORD, PASSWORD)).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.name").value("Ana Silva"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        JsonNode user = bodyOf(response).get("user");
        Map<String, Object> organization = jdbc.queryForMap(
                "select name, plan, trial_ends_at from organizations where id = ?", user.get("organizationId").asText());
        assertThat(organization.get("name")).isEqualTo("Escritorio Silva");
        assertThat(organization.get("plan")).isEqualTo("trial");
        Instant trialEnds = ((java.sql.Timestamp) organization.get("trial_ends_at")).toInstant();
        assertThat(Duration.between(Instant.now(), trialEnds).toDays()).isBetween(28L, 30L);

        Map<String, Object> stored = jdbc.queryForMap(
                "select email, role, totp_enabled, password_hash from users where id = ?", user.get("id").asText());
        assertThat(stored.get("email")).isEqualTo(email);
        assertThat(stored.get("role")).isEqualTo("ADMIN");
        assertThat(stored.get("totp_enabled")).isEqualTo(false);
        assertThat(passwordEncoder.matches(PASSWORD, (String) stored.get("password_hash"))).isTrue();
    }

    @Test
    @DisplayName("register sets the refresh cookie and stores only its hash")
    void registerSetsRefreshCookie() throws Exception {
        ResultActions response = postPublic("/api/auth/register", registration(uniqueEmail(), PASSWORD, PASSWORD))
                .andExpect(status().isOk());

        String setCookie = response.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith(REFRESH_COOKIE + "=").contains("HttpOnly", "Secure", "SameSite=None",
                "Path=/api/auth", "Max-Age=" + Duration.ofDays(30).toSeconds());
        String raw = response.andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();
        String userId = bodyOf(response).get("user").get("id").asText();
        Map<String, Object> row = jdbc.queryForMap(
                "select token_hash, revoked_at, expires_at from refresh_tokens where user_id = ?", userId);
        assertThat(row.get("token_hash")).isEqualTo(sha256(raw)).isNotEqualTo(raw);
        assertThat(row.get("revoked_at")).isNull();
        Instant expires = ((java.sql.Timestamp) row.get("expires_at")).toInstant();
        assertThat(Duration.between(Instant.now(), expires).toDays()).isBetween(28L, 30L);
    }

    @Test
    @DisplayName("the access token carries the user claims and expires in 15 minutes")
    void accessTokenClaims() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);

        Claims claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(AUTH_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(account.accessToken()).getPayload();

        assertThat(claims.getSubject()).isEqualTo(account.userId());
        assertThat(claims.get("organizationId", String.class)).isEqualTo(account.orgId());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("name", String.class)).isEqualTo("Ana Admin");
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("register rejects different passwords")
    void registerRejectsPasswordMismatch() throws Exception {
        postPublic("/api/auth/register", registration(uniqueEmail(), PASSWORD, "outra-senha-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SENHAS_NAO_COINCIDEM"))
                .andExpect(jsonPath("$.error").value("As senhas não coincidem"));
    }

    @Test
    @DisplayName("register rejects an email that is already used, ignoring case")
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        postPublic("/api/auth/register", registration(email.toUpperCase(), PASSWORD, PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EM_USO"))
                .andExpect(jsonPath("$.error").value("Já existe um usuário com este email"));
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, email))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("register validates the request fields")
    void registerValidatesFields() throws Exception {
        postPublic("/api/auth/register", registration("not-an-email", "short", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Email inválido")))
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Senha deve ter ao menos 8 caracteres")));
    }

    @Test
    @DisplayName("login returns the session and a new refresh cookie")
    void loginSucceeds() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);

        ResultActions response = login(email, PASSWORD, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(account.userId()))
                .andExpect(jsonPath("$.user.organizationId").value(account.orgId()))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(response.andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue()).isNotBlank();
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ?", Integer.class,
                account.userId())).isEqualTo(2);
    }

    @Test
    @DisplayName("login accepts the email with other casing")
    void loginNormalizesEmail() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        login(email.toUpperCase(), PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a wrong password and an unknown email answer the same way")
    void loginHidesWhichPartIsWrong() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        login(email, "senha-errada-123", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.error").value("Email ou senha inválidos"));
        login(uniqueEmail(), PASSWORD, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.error").value("Email ou senha inválidos"));
    }

    @Test
    @DisplayName("login validates the request fields")
    void loginValidatesFields() throws Exception {
        login("not-an-email", "", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("a user with two factor needs the code, and a wrong code is refused")
    void loginWithTwoFactor() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);
        String secret = enableTwoFactor(account);

        login(email, PASSWORD, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"))
                .andExpect(jsonPath("$.error").value("Código de verificação em dois fatores obrigatório"));
        login(email, PASSWORD, "   ").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));
        login(email, PASSWORD, wrongTotpCode(secret)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_INVALID"))
                .andExpect(jsonPath("$.error").value("Código de verificação inválido"));
        login(email, PASSWORD, " " + totpCodeFor(secret) + " ").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(account.userId()));
    }

    @Test
    @DisplayName("the password is checked before the two factor code is asked for")
    void wrongPasswordWinsOverTwoFactor() throws Exception {
        String email = uniqueEmail();
        enableTwoFactor(registerAdmin(email));

        login(email, "senha-errada-123", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    @DisplayName("login counts attempts per email and per client address with a 15 minute window")
    void loginCountsAttempts() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        login(email, PASSWORD, null).andExpect(status().isOk());

        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:email:" + email)),
                eq("900"));
        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:ip:127.0.0.1")),
                eq("900"));
    }

    @Test
    @DisplayName("login uses the last address of X-Forwarded-For as the client address")
    void loginUsesLastForwardedAddress() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        mvc.perform(post("/api/auth/login").header("X-Forwarded-For", "203.0.113.5, 198.51.100.7")
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk());

        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("ratelimit:login:ip:198.51.100.7")), eq("900"));
    }

    @Test
    @DisplayName("a successful login clears both counters")
    void loginClearsCounters() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        login(email, PASSWORD, null).andExpect(status().isOk());

        verify(redis).delete("ratelimit:login:email:" + email);
        verify(redis).delete("ratelimit:login:ip:127.0.0.1");
    }

    @Test
    @DisplayName("a failed login does not clear the counters")
    void failedLoginKeepsCounters() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);

        login(email, "senha-errada-123", null).andExpect(status().isUnauthorized());

        org.mockito.Mockito.verify(redis, org.mockito.Mockito.never()).delete(anyString());
    }

    @Test
    @DisplayName("too many attempts are refused as invalid credentials, even with the right password")
    void loginRateLimited() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);
        limitReachedFor(11L);

        login(email, PASSWORD, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.error").value("Email ou senha inválidos"));
    }

    @Test
    @DisplayName("the counter limit is 10 per email")
    void loginLimitIsTenPerEmail() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:email:" + email)),
                ArgumentMatchers.<Object>any())).thenReturn(10L);
        login(email, PASSWORD, null).andExpect(status().isOk());

        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:email:" + email)),
                ArgumentMatchers.<Object>any())).thenReturn(11L);
        login(email, PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the counter limit is 30 per address")
    void loginLimitIsThirtyPerAddress() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:ip:127.0.0.1")),
                ArgumentMatchers.<Object>any())).thenReturn(30L);
        login(email, PASSWORD, null).andExpect(status().isOk());

        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of("ratelimit:login:ip:127.0.0.1")),
                ArgumentMatchers.<Object>any())).thenReturn(31L);
        login(email, PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login fails closed when Redis is down")
    void loginFailsClosedWithoutRedis() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), ArgumentMatchers.<Object>any()))
                .thenThrow(new IllegalStateException("redis down"));

        login(email, PASSWORD, null).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"))
                .andExpect(jsonPath("$.error").value("Não foi possível validar o acesso agora. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("a failure to clear the counters does not fail the login")
    void loginSurvivesClearFailure() throws Exception {
        String email = uniqueEmail();
        registerAdmin(email);
        when(redis.delete(anyString())).thenThrow(new IllegalStateException("redis down"));

        login(email, PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh rotates the token: the old one is revoked and a new session is returned")
    void refreshRotates() throws Exception {
        Account account = registerAdmin();

        ResultActions response = refreshWith(account.refreshToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(account.userId()))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        String newToken = response.andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();
        assertThat(newToken).isNotEqualTo(account.refreshToken());
        assertThat(jdbc.queryForObject("select revoked_at is not null from refresh_tokens where token_hash = ?",
                Boolean.class, sha256(account.refreshToken()))).isTrue();
        assertThat(jdbc.queryForObject("select revoked_at is null from refresh_tokens where token_hash = ?",
                Boolean.class, sha256(newToken))).isTrue();
        refreshWith(newToken).andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh reflects the current role of the user")
    void refreshUsesCurrentRole() throws Exception {
        Account account = registerAdmin();
        jdbc.update("update users set role = 'ADVOGADO' where id = ?", account.userId());

        refreshWith(account.refreshToken()).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADVOGADO"));
    }

    @Test
    @DisplayName("refresh without a cookie is refused")
    void refreshWithoutCookie() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALIDO"))
                .andExpect(jsonPath("$.error").value("Sessão inválida. Faça login novamente."));
    }

    @Test
    @DisplayName("refresh with an unknown token is refused")
    void refreshWithUnknownToken() throws Exception {
        refreshWith("token-que-nunca-existiu").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALIDO"));
    }

    @Test
    @DisplayName("refresh with an expired token is refused with its own code")
    void refreshWithExpiredToken() throws Exception {
        Account account = registerAdmin();
        jdbc.update("update refresh_tokens set expires_at = now() - interval '1 day' where token_hash = ?",
                sha256(account.refreshToken()));

        refreshWith(account.refreshToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_EXPIRADO"))
                .andExpect(jsonPath("$.error").value("Sessão expirada. Faça login novamente."));
    }

    @Test
    @DisplayName("presenting a revoked token again revokes every session of the user")
    void reusedTokenRevokesAllSessions() throws Exception {
        Account account = registerAdmin();
        String rotated = refreshWith(account.refreshToken()).andExpect(status().isOk()).andReturn().getResponse()
                .getCookie(REFRESH_COOKIE).getValue();

        refreshWith(account.refreshToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALIDO"));

        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Integer.class, account.userId())).isZero();
        refreshWith(rotated).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALIDO"));
    }

    @Test
    @DisplayName("refresh for a user that no longer exists is refused and the token stays revoked")
    void refreshForDeletedUser() throws Exception {
        Account account = registerAdmin();
        jdbc.update("delete from users where id = ?", account.userId());

        refreshWith(account.refreshToken()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALIDO"));

        assertThat(jdbc.queryForObject("select revoked_at is not null from refresh_tokens where token_hash = ?",
                Boolean.class, sha256(account.refreshToken()))).isTrue();
    }

    @Test
    @DisplayName("logout revokes the token and clears the cookie")
    void logoutRevokesToken() throws Exception {
        Account account = registerAdmin();

        ResultActions response = mvc.perform(post("/api/auth/logout").cookie(new Cookie(REFRESH_COOKIE,
                account.refreshToken()))).andExpect(status().isNoContent());

        String setCookie = response.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith(REFRESH_COOKIE + "=;").contains("Max-Age=0", "HttpOnly", "Secure",
                "Path=/api/auth");
        assertThat(jdbc.queryForObject("select revoked_at is not null from refresh_tokens where token_hash = ?",
                Boolean.class, sha256(account.refreshToken()))).isTrue();
        refreshWith(account.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout only revokes the presented token, not the other sessions")
    void logoutKeepsOtherSessions() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);
        String other = login(email, PASSWORD, null).andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();

        mvc.perform(post("/api/auth/logout").cookie(new Cookie(REFRESH_COOKIE, account.refreshToken())))
                .andExpect(status().isNoContent());

        refreshWith(other).andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout works without a cookie or with an unknown token")
    void logoutIsForgiving() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/logout").cookie(new Cookie(REFRESH_COOKIE, "desconhecido")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the auth endpoints are public")
    void authEndpointsArePublic() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", "x@y.com");
        body.put("password", "x");
        postPublic("/api/auth/login", body).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }
}
