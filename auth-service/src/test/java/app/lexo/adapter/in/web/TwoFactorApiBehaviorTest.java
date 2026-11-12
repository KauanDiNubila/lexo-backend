package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import app.lexo.common.event.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Two factor API")
class TwoFactorApiBehaviorTest extends IntegrationTestBase {

    private Map<String, Object> userRow(String userId) {
        return jdbc.queryForMap("select totp_secret, totp_enabled, totp_pending_secret from users where id = ?",
                userId);
    }

    @Test
    @DisplayName("starting returns a secret and an otpauth link, and keeps the secret encrypted as pending")
    void startsTheSetup() throws Exception {
        Account account = registerAdmin();

        JsonNode body = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of()).andExpect(status().isOk()));

        String secret = body.get("secret").asText();
        assertThat(secret).matches("[A-Z2-7]{32}");
        String label = URLEncoder.encode("Lexo:" + account.email(), StandardCharsets.UTF_8);
        assertThat(body.get("otpauthUri").asText()).isEqualTo("otpauth://totp/" + label + "?secret=" + secret
                + "&issuer=Lexo&algorithm=SHA1&digits=6&period=30");
        Map<String, Object> row = userRow(account.userId());
        assertThat((String) row.get("totp_pending_secret")).startsWith("enc:v1:").doesNotContain(secret);
        assertThat(row.get("totp_secret")).isNull();
        assertThat(row.get("totp_enabled")).isEqualTo(false);
        assertThat(publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("starting again replaces the pending secret")
    void startingAgainReplacesThePendingSecret() throws Exception {
        Account account = registerAdmin();
        String first = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of())).get("secret").asText();
        String second = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of())).get("secret").asText();
        assertThat(second).isNotEqualTo(first);

        postAs(account, "/api/2fa/confirmar", Map.of("code", totpCodeFor(first))).andExpect(status().isBadRequest());
        postAs(account, "/api/2fa/confirmar", Map.of("code", totpCodeFor(second))).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("confirming with the right code turns two factor on and records the event")
    void confirmsTheSetup() throws Exception {
        Account account = registerAdmin();
        String secret = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of())).get("secret").asText();
        String pending = (String) userRow(account.userId()).get("totp_pending_secret");

        postAs(account, "/api/2fa/confirmar", Map.of("code", totpCodeFor(secret))).andExpect(status().isNoContent());

        Map<String, Object> row = userRow(account.userId());
        assertThat(row.get("totp_enabled")).isEqualTo(true);
        assertThat(row.get("totp_secret")).isEqualTo(pending);
        assertThat(row.get("totp_pending_secret")).isNull();
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("ATIVOU_2FA");
        assertThat(event.organizationId()).isEqualTo(account.orgId());
        assertThat(event.entityType()).isNull();
        assertThat(event.entityId()).isNull();
        assertThat(event.description()).isEqualTo("Ativou verificação em dois fatores");
        assertThat(event.userId()).isEqualTo(account.userId());
        assertThat(event.userName()).isEqualTo("Ana Admin");
    }

    @Test
    @DisplayName("confirming with a wrong code is refused and keeps the setup pending")
    void refusesAWrongConfirmation() throws Exception {
        Account account = registerAdmin();
        String secret = bodyOf(postAs(account, "/api/2fa/iniciar", Map.of())).get("secret").asText();

        postAs(account, "/api/2fa/confirmar", Map.of("code", wrongTotpCode(secret)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Código inválido. Tente novamente."));

        Map<String, Object> row = userRow(account.userId());
        assertThat(row.get("totp_enabled")).isEqualTo(false);
        assertThat(row.get("totp_pending_secret")).isNotNull();
        assertThat(publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("confirming without starting asks to restart the process")
    void confirmingWithoutStartingIsRefused() throws Exception {
        Account account = registerAdmin();

        postAs(account, "/api/2fa/confirmar", Map.of("code", "123456")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sessão expirada. Reinicie o processo."));
    }

    @Test
    @DisplayName("the code is required")
    void theCodeIsRequired() throws Exception {
        Account account = registerAdmin();

        postAs(account, "/api/2fa/confirmar", Map.of("code", "")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Código é obrigatório"));
        postAs(account, "/api/2fa/desativar", Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("disabling with the right code clears the secrets and records the event")
    void disablesTwoFactor() throws Exception {
        Account account = registerAdmin();
        String secret = enableTwoFactor(account);
        Mockito.clearInvocations(events);

        postAs(account, "/api/2fa/desativar", Map.of("code", totpCodeFor(secret))).andExpect(status().isNoContent());

        Map<String, Object> row = userRow(account.userId());
        assertThat(row.get("totp_enabled")).isEqualTo(false);
        assertThat(row.get("totp_secret")).isNull();
        assertThat(row.get("totp_pending_secret")).isNull();
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("DESATIVOU_2FA");
        assertThat(event.entityType()).isNull();
        assertThat(event.entityId()).isNull();
        assertThat(event.description()).isEqualTo("Desativou verificação em dois fatores");
        assertThat(event.userId()).isEqualTo(account.userId());
        login(account.email(), PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("disabling with a wrong code is refused")
    void refusesAWrongDisable() throws Exception {
        Account account = registerAdmin();
        String secret = enableTwoFactor(account);

        postAs(account, "/api/2fa/desativar", Map.of("code", wrongTotpCode(secret)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Código inválido."));
        assertThat(userRow(account.userId()).get("totp_enabled")).isEqualTo(true);
    }

    @Test
    @DisplayName("disabling when two factor is off is refused")
    void disablingWhenOffIsRefused() throws Exception {
        Account account = registerAdmin();

        postAs(account, "/api/2fa/desativar", Map.of("code", "123456")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("2FA não está ativado."));
    }

    @Test
    @DisplayName("a secret stored in plain text before encryption existed still works at login")
    void legacyPlainSecretStillWorks() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);
        String legacySecret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        jdbc.update("update users set totp_secret = ?, totp_enabled = true where id = ?", legacySecret,
                account.userId());

        login(email, PASSWORD, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));
        login(email, PASSWORD, totpCodeFor(legacySecret)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("two factor enabled without a stored secret does not ask for a code")
    void enabledWithoutSecretDoesNotAsk() throws Exception {
        String email = uniqueEmail();
        Account account = registerAdmin(email);
        jdbc.update("update users set totp_enabled = true where id = ?", account.userId());

        login(email, PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("a caller that does not exist anymore is unauthorized")
    void unknownCallerIsUnauthorized() throws Exception {
        Account admin = registerAdmin();
        Account ghost = ghost(admin.orgId());

        postAs(ghost, "/api/2fa/iniciar", Map.of()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NAO_AUTORIZADO"))
                .andExpect(jsonPath("$.error").value("Usuário não encontrado"));
        postAs(ghost, "/api/2fa/confirmar", Map.of("code", "123456")).andExpect(status().isUnauthorized());
        postAs(ghost, "/api/2fa/desativar", Map.of("code", "123456")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("every role can manage their own two factor")
    void everyRoleCanUseIt() throws Exception {
        Account admin = registerAdmin();
        Account secretary = insertMember(admin, "SECRETARIA");

        postAs(secretary, "/api/2fa/iniciar", Map.of()).andExpect(status().isOk());
    }
}
