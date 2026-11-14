package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Invite API: info and acceptance")
class InviteAcceptanceApiBehaviorTest extends IntegrationTestBase {

    private static final String NEW_PASSWORD = "nova-senha-123";

    private record Invite(String id, String token, String email) {
    }

    private Invite invite(Account admin, String role) throws Exception {
        String email = uniqueEmail();
        String id = bodyOf(postAs(admin, "/api/usuarios/convites",
                Map.of("name", "Carla Nova", "email", email, "role", role))
                .andExpect(status().isCreated())).get("id").asText();
        String token = jdbc.queryForObject("select token from user_invites where id = ?", String.class, id);
        return new Invite(id, token, email);
    }

    private Map<String, Object> acceptance(String token, String password, String confirmation) {
        return Map.of("token", token, "password", password, "confirmPassword", confirmation);
    }

    @Test
    @DisplayName("info of a valid invite shows the organization, no login needed")
    void infoOfAValidInvite() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");

        mvc.perform(get("/api/convites/info/" + invite.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.organizationName").value("Escritorio Teste"))
                .andExpect(jsonPath("$.inviteeName").value("Carla Nova"))
                .andExpect(jsonPath("$.email").value(invite.email()))
                .andExpect(jsonPath("$.reason").value(nullValue()));
    }

    @Test
    @DisplayName("info of an unknown token says it is invalid")
    void infoOfUnknownToken() throws Exception {
        mvc.perform(get("/api/convites/info/token-inexistente")).andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("Convite inválido"))
                .andExpect(jsonPath("$.organizationName").value(nullValue()))
                .andExpect(jsonPath("$.inviteeName").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()));
    }

    @Test
    @DisplayName("info of a used invite says it was used, still showing the invitee")
    void infoOfAUsedInvite() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");
        jdbc.update("update user_invites set accepted_at = now() where id = ?", invite.id());

        mvc.perform(get("/api/convites/info/" + invite.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("Este convite já foi utilizado"))
                .andExpect(jsonPath("$.inviteeName").value("Carla Nova"))
                .andExpect(jsonPath("$.email").value(invite.email()))
                .andExpect(jsonPath("$.organizationName").value(nullValue()));
    }

    @Test
    @DisplayName("info of an expired invite says it expired")
    void infoOfAnExpiredInvite() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");
        jdbc.update("update user_invites set expires_at = now() - interval '1 day' where id = ?", invite.id());

        mvc.perform(get("/api/convites/info/" + invite.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("Este convite expirou"))
                .andExpect(jsonPath("$.inviteeName").value("Carla Nova"));
    }

    @Test
    @DisplayName("a used invite is reported as used even if it also expired")
    void usedWinsOverExpired() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");
        jdbc.update("update user_invites set accepted_at = now(), expires_at = now() - interval '1 day' "
                + "where id = ?", invite.id());

        mvc.perform(get("/api/convites/info/" + invite.token()))
                .andExpect(jsonPath("$.reason").value("Este convite já foi utilizado"));
    }

    @Test
    @DisplayName("accepting creates the user with the invited role and marks the invite as used")
    void acceptsAnInvite() throws Exception {
        Account admin = registerAdmin();
        Invite invite = invite(admin, "SECRETARIA");

        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        Map<String, Object> user = jdbc.queryForMap(
                "select id, organization_id, name, email, role, password_hash, totp_enabled from users where email = ?",
                invite.email());
        assertThat(user.get("organization_id")).isEqualTo(admin.orgId());
        assertThat(user.get("name")).isEqualTo("Carla Nova");
        assertThat(user.get("role")).isEqualTo("SECRETARIA");
        assertThat(user.get("totp_enabled")).isEqualTo(false);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, (String) user.get("password_hash"))).isTrue();
        assertThat(jdbc.queryForObject("select accepted_at is not null from user_invites where id = ?",
                Boolean.class, invite.id())).isTrue();
        login(invite.email(), NEW_PASSWORD, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SECRETARIA"))
                .andExpect(jsonPath("$.user.organizationId").value(admin.orgId()));
    }

    @Test
    @DisplayName("an invite can be accepted only once")
    void acceptsOnlyOnce() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");
        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isNoContent());

        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Este convite já foi utilizado"));
    }

    @Test
    @DisplayName("accepting refuses different passwords before looking at the invite")
    void refusesPasswordMismatch() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");

        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, "outra-senha-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("As senhas não coincidem"));
        postPublic("/api/convites/aceitar", acceptance("token-inexistente", NEW_PASSWORD, "outra-senha-123"))
                .andExpect(jsonPath("$.error").value("As senhas não coincidem"));
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, invite.email()))
                .isZero();
    }

    @Test
    @DisplayName("accepting refuses an unknown or expired invite")
    void refusesUnknownOrExpired() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");

        postPublic("/api/convites/aceitar", acceptance("token-inexistente", NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Convite inválido"));

        jdbc.update("update user_invites set expires_at = now() - interval '1 day' where id = ?", invite.id());
        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Este convite expirou"));
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, invite.email()))
                .isZero();
    }

    @Test
    @DisplayName("accepting is a conflict when the email already has an account")
    void conflictsWhenTheAccountExists() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");
        insertMember(registerAdmin().orgId(), "Outra Pessoa", invite.email(), "ADVOGADO");

        postPublic("/api/convites/aceitar", acceptance(invite.token(), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLITO"))
                .andExpect(jsonPath("$.error").value("Já existe uma conta com este email"));
        assertThat(jdbc.queryForObject("select accepted_at is null from user_invites where id = ?",
                Boolean.class, invite.id())).isTrue();
    }

    @Test
    @DisplayName("accepting validates the request fields")
    void validatesTheRequest() throws Exception {
        Invite invite = invite(registerAdmin(), "ADVOGADO");

        postPublic("/api/convites/aceitar", acceptance(invite.token(), "curta", "curta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Senha deve ter ao menos 8 caracteres"));
        postPublic("/api/convites/aceitar", Map.of("password", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO"));
    }
}
