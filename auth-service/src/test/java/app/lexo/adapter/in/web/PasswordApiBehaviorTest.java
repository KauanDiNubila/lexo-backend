package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import app.lexo.common.email.EmailTask;
import app.lexo.common.email.RabbitConfig;
import app.lexo.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Password API: forgot, reset and change")
class PasswordApiBehaviorTest extends IntegrationTestBase {

    private static final String NEW_PASSWORD = "another-password-2";
    private static final String LINK_PREFIX = "http://localhost:8090/redefinir-senha/";

    private List<EmailTask> sentEmails() {
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbit, atLeast(0)).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                message.capture());
        return message.getAllValues().stream().map(EmailTask.class::cast).toList();
    }

    private List<EmailTask> sentTo(String email) {
        return sentEmails().stream().filter(task -> task.recipient().equals(email)).toList();
    }

    private String tokenIn(EmailTask task) {
        String body = task.body();
        int start = body.indexOf(LINK_PREFIX) + LINK_PREFIX.length();
        return body.substring(start).split(" ")[0];
    }

    private String forgot(String email) throws Exception {
        postPublic("/api/auth/senha/esqueci", Map.of("email", email)).andExpect(status().isNoContent());
        List<EmailTask> sent = sentTo(email.toLowerCase());
        return tokenIn(sent.get(sent.size() - 1));
    }

    private void reset(String token, String password, String confirmation) throws Exception {
        postPublic("/api/auth/senha/redefinir",
                Map.of("token", token, "password", password, "confirmPassword", confirmation))
                .andExpect(status().isNoContent());
    }

    private String sha256(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean refreshTokensAreRevoked(String userId) {
        return jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Integer.class, userId) == 0;
    }

    private int pendingResets(String userId) {
        return jdbc.queryForObject("select count(*) from password_resets where user_id = ?", Integer.class, userId);
    }

    private void limitReached(String key, long count) {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)),
                ArgumentMatchers.<Object>any())).thenReturn(count);
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(Account account, String current,
                                                                              String password,
                                                                              String confirmation) throws Exception {
        return putAs(account, "/api/conta/senha",
                Map.of("currentPassword", current, "newPassword", password, "confirmPassword", confirmation));
    }

    @Test
    @DisplayName("asking for a link answers the same for an email with an account and one without")
    void forgotAnswersTheSame() throws Exception {
        Account admin = registerAdmin();

        postPublic("/api/auth/senha/esqueci", Map.of("email", admin.email())).andExpect(status().isNoContent());
        postPublic("/api/auth/senha/esqueci", Map.of("email", "nobody@escritorio.com"))
                .andExpect(status().isNoContent());

        assertThat(sentEmails()).hasSize(1);
        assertThat(sentEmails().get(0).recipient()).isEqualTo(admin.email());
    }

    @Test
    @DisplayName("the link email carries a token stored only as a hash, valid for one hour")
    void forgotStoresTheHash() throws Exception {
        Account admin = registerAdmin(uniqueEmail().toUpperCase());

        String token = forgot(admin.email().toUpperCase());

        EmailTask task = sentTo(admin.email()).get(0);
        assertThat(task.type()).isEqualTo("REDEFINICAO_SENHA");
        assertThat(task.subject()).isEqualTo("Redefinição de senha no Lexo");
        assertThat(task.body()).startsWith("Olá Ana Admin, recebemos um pedido para redefinir a sua senha.")
                .contains(LINK_PREFIX + token + " O link vale por 1 hora");
        Map<String, Object> row = jdbc.queryForMap("select user_id, token_hash, "
                + "extract(epoch from (expires_at - created_at)) as seconds from password_resets where user_id = ?",
                admin.userId());
        assertThat(row.get("token_hash")).isEqualTo(sha256(token));
        assertThat(((Number) row.get("seconds")).doubleValue()).isBetween(3595.0, 3605.0);
        assertThat(pendingResets(admin.userId())).isEqualTo(1);
    }

    @Test
    @DisplayName("a new request replaces the previous link, which stops working")
    void newRequestReplacesThePrevious() throws Exception {
        Account admin = registerAdmin();
        String first = forgot(admin.email());
        String second = forgot(admin.email());

        assertThat(pendingResets(admin.userId())).isEqualTo(1);
        postPublic("/api/auth/senha/redefinir",
                Map.of("token", first, "password", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest());
        reset(second, NEW_PASSWORD, NEW_PASSWORD);
    }

    @Test
    @DisplayName("resetting changes the password, ends every session, records the event and warns by email")
    void resetFlow() throws Exception {
        Account admin = registerAdmin();
        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
        String token = forgot(admin.email());

        reset(token, NEW_PASSWORD, NEW_PASSWORD);

        assertThat(refreshTokensAreRevoked(admin.userId())).isTrue();
        refreshWith(admin.refreshToken()).andExpect(status().isUnauthorized());
        login(admin.email(), PASSWORD, null).andExpect(status().isUnauthorized());
        login(admin.email(), NEW_PASSWORD, null).andExpect(status().isOk());
        verify(redisValues).set(eq("session-cutoff:" + admin.userId()), anyString(), eq(Duration.ofMinutes(16)));
        assertThat(pendingResets(admin.userId())).isZero();
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("REDEFINIU_SENHA");
        assertThat(event.organizationId()).isEqualTo(admin.orgId());
        assertThat(event.userId()).isEqualTo(admin.userId());
        assertThat(event.userName()).isEqualTo("Ana Admin");
        List<EmailTask> sent = sentTo(admin.email());
        assertThat(sent).hasSize(2);
        assertThat(sent.get(1).type()).isEqualTo("SENHA_ALTERADA");
        assertThat(sent.get(1).subject()).isEqualTo("Sua senha do Lexo foi alterada");
    }

    @Test
    @DisplayName("a token works only once")
    void tokenIsSingleUse() throws Exception {
        Account admin = registerAdmin();
        String token = forgot(admin.email());
        reset(token, NEW_PASSWORD, NEW_PASSWORD);

        postPublic("/api/auth/senha/redefinir",
                Map.of("token", token, "password", "third-password-3", "confirmPassword", "third-password-3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));

        login(admin.email(), NEW_PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("an expired token and an unknown token are refused with the same message")
    void invalidTokens() throws Exception {
        Account admin = registerAdmin();
        String token = forgot(admin.email());
        jdbc.update("update password_resets set expires_at = now() - interval '1 minute' where user_id = ?",
                admin.userId());

        for (String candidate : List.of(token, "never-issued")) {
            postPublic("/api/auth/senha/redefinir",
                    Map.of("token", candidate, "password", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));
        }
        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a confirmation that does not match, a short password and a blank token are refused")
    void invalidBodies() throws Exception {
        Account admin = registerAdmin();
        String token = forgot(admin.email());

        postPublic("/api/auth/senha/redefinir",
                Map.of("token", token, "password", NEW_PASSWORD, "confirmPassword", "something-else-3"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SENHAS_NAO_COINCIDEM"));
        postPublic("/api/auth/senha/redefinir",
                Map.of("token", token, "password", "short", "confirmPassword", "short"))
                .andExpect(status().isBadRequest());
        postPublic("/api/auth/senha/redefinir",
                Map.of("token", " ", "password", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest());
        postPublic("/api/auth/senha/esqueci", Map.of("email", "not-an-email")).andExpect(status().isBadRequest());

        assertThat(pendingResets(admin.userId())).isEqualTo(1);
        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("resetting the password does not turn two factor off")
    void resetKeepsTwoFactor() throws Exception {
        Account admin = registerAdmin();
        enableTwoFactor(admin);
        String token = forgot(admin.email());

        reset(token, NEW_PASSWORD, NEW_PASSWORD);

        login(admin.email(), NEW_PASSWORD, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));
    }

    @Test
    @DisplayName("resetting is refused and undone when the session cutoff cannot be recorded")
    void resetFailsClosedWithoutRedis() throws Exception {
        Account admin = registerAdmin();
        String token = forgot(admin.email());
        doThrow(new IllegalStateException("redis down")).when(redisValues)
                .set(anyString(), anyString(), any(Duration.class));

        postPublic("/api/auth/senha/redefinir",
                Map.of("token", token, "password", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"));

        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
        assertThat(pendingResets(admin.userId())).isEqualTo(1);
        assertThat(publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("asking for a link is limited to three per hour for each email")
    void forgotIsLimited() throws Exception {
        Account admin = registerAdmin();
        limitReached("ratelimit:password-reset:email:" + admin.email(), 3L);
        postPublic("/api/auth/senha/esqueci", Map.of("email", admin.email())).andExpect(status().isNoContent());
        assertThat(sentTo(admin.email())).hasSize(1);

        limitReached("ratelimit:password-reset:email:" + admin.email(), 4L);
        postPublic("/api/auth/senha/esqueci", Map.of("email", admin.email())).andExpect(status().isNoContent());

        assertThat(sentTo(admin.email())).hasSize(1);
    }

    @Test
    @DisplayName("asking for a link fails closed when Redis is down, for any email")
    void forgotFailsClosedWithoutRedis() throws Exception {
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.anyList(),
                ArgumentMatchers.<Object>any())).thenThrow(new IllegalStateException("redis down"));

        postPublic("/api/auth/senha/esqueci", Map.of("email", "nobody@escritorio.com"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"));
    }

    @Test
    @DisplayName("asking for a link still answers 204 when the email queue is down")
    void forgotWithTheQueueDown() throws Exception {
        Account admin = registerAdmin();
        doThrow(new IllegalStateException("rabbit down")).when(rabbit)
                .convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                        ArgumentMatchers.<Object>any());

        postPublic("/api/auth/senha/esqueci", Map.of("email", admin.email())).andExpect(status().isNoContent());
        postPublic("/api/auth/senha/esqueci", Map.of("email", "nobody@escritorio.com"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("removing a user removes the pending reset link")
    void removingAUserRemovesTheirResets() throws Exception {
        Account admin = registerAdmin();
        Account member = insertMember(admin, "ADVOGADO");
        forgot(member.email());
        assertThat(pendingResets(member.userId())).isEqualTo(1);

        deleteAs(admin, "/api/usuarios/" + member.userId()).andExpect(status().isNoContent());

        assertThat(pendingResets(member.userId())).isZero();
    }

    @Test
    @DisplayName("changing the password checks the current one, ends every session and records the event")
    void changeFlow() throws Exception {
        Account admin = registerAdmin();
        login(admin.email(), PASSWORD, null).andExpect(status().isOk());

        changePassword(admin, PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        login(admin.email(), PASSWORD, null).andExpect(status().isUnauthorized());
        login(admin.email(), NEW_PASSWORD, null).andExpect(status().isOk());
        refreshWith(admin.refreshToken()).andExpect(status().isUnauthorized());
        verify(redisValues).set(eq("session-cutoff:" + admin.userId()), anyString(), eq(Duration.ofMinutes(16)));
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("ALTEROU_SENHA");
        assertThat(event.organizationId()).isEqualTo(admin.orgId());
        assertThat(event.userId()).isEqualTo(admin.userId());
        assertThat(event.userName()).isEqualTo("Ana Admin");
        assertThat(sentTo(admin.email())).hasSize(1);
        assertThat(sentTo(admin.email()).get(0).type()).isEqualTo("SENHA_ALTERADA");
        verify(redis).delete("ratelimit:password-change:" + admin.userId());
    }

    @Test
    @DisplayName("a wrong current password, a different confirmation, the same password and a short one are refused")
    void changeRefusals() throws Exception {
        Account admin = registerAdmin();

        changePassword(admin, "not-my-password", NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Senha atual incorreta"));
        changePassword(admin, PASSWORD, NEW_PASSWORD, "something-else-3").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SENHAS_NAO_COINCIDEM"));
        changePassword(admin, PASSWORD, PASSWORD, PASSWORD).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A nova senha deve ser diferente da atual"));
        changePassword(admin, PASSWORD, "short", "short").andExpect(status().isBadRequest());

        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
        assertThat(publishedEvents()).isEmpty();
        assertThat(sentEmails()).isEmpty();
    }

    @Test
    @DisplayName("changing the password needs a signed-in user")
    void changeNeedsAuthentication() throws Exception {
        mvc.perform(put("/api/conta/senha").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD,
                        "confirmPassword", NEW_PASSWORD)))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("changing the password is limited to five attempts in fifteen minutes")
    void changeIsLimited() throws Exception {
        Account admin = registerAdmin();
        limitReached("ratelimit:password-change:" + admin.userId(), 5L);
        changePassword(admin, "not-my-password", NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isBadRequest());

        limitReached("ratelimit:password-change:" + admin.userId(), 6L);
        changePassword(admin, PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("REQUISICOES_EM_EXCESSO"));

        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("changing the password is refused and undone when the session cutoff cannot be recorded")
    void changeFailsClosedWithoutRedis() throws Exception {
        Account admin = registerAdmin();
        doThrow(new IllegalStateException("redis down")).when(redisValues)
                .set(anyString(), anyString(), any(Duration.class));

        changePassword(admin, PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICO_INDISPONIVEL"));

        login(admin.email(), PASSWORD, null).andExpect(status().isOk());
        assertThat(publishedEvents()).isEmpty();
    }
}
