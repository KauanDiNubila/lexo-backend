package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import app.lexo.common.email.RabbitConfig;
import app.lexo.common.email.TarefaEmail;
import app.lexo.common.event.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Team API: users and invites")
class TeamApiBehaviorTest extends IntegrationTestBase {

    private Map<String, Object> invitation(String email, String role) {
        return Map.of("name", "Carla Nova", "email", email, "role", role);
    }

    private String inviteId(Account admin, String email) throws Exception {
        return bodyOf(postAs(admin, "/api/usuarios/convites", invitation(email, "ADVOGADO"))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private void expireInvite(String inviteId) {
        jdbc.update("update user_invites set expires_at = now() - interval '1 day' where id = ?", inviteId);
    }

    private boolean refreshTokensAreRevoked(String userId) {
        return jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ? and revoked_at is null",
                Integer.class, userId) == 0;
    }

    @Test
    @DisplayName("an admin lists only the users of the own organization, ordered by name")
    void listsUsersOfTheOrganization() throws Exception {
        Account admin = registerAdmin();
        insertMember(admin.orgId(), "Carlos Lima", uniqueEmail(), "ADVOGADO");
        insertMember(admin.orgId(), "Bruno Souza", uniqueEmail(), "SECRETARIA");
        registerAdmin();

        JsonNode users = bodyOf(getAs(admin, "/api/usuarios").andExpect(status().isOk()));

        assertThat(users).hasSize(3);
        assertThat(users.get(0).get("name").asText()).isEqualTo("Ana Admin");
        assertThat(users.get(1).get("name").asText()).isEqualTo("Bruno Souza");
        assertThat(users.get(2).get("name").asText()).isEqualTo("Carlos Lima");
        assertThat(users.get(1).get("role").asText()).isEqualTo("SECRETARIA");
        assertThat(users.get(1).get("totpEnabled").asBoolean()).isFalse();
        assertThat(users.get(1).get("createdAt").asText()).isNotBlank();
        assertThat(users.get(1).has("passwordHash")).isFalse();
    }

    @Test
    @DisplayName("only admins can use the team endpoints")
    void nonAdminsAreRefused() throws Exception {
        Account admin = registerAdmin();
        Account lawyer = insertMember(admin, "ADVOGADO");
        Account secretary = insertMember(admin, "SECRETARIA");

        for (Account member : new Account[] {lawyer, secretary}) {
            getAs(member, "/api/usuarios").andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
            getAs(member, "/api/usuarios/convites").andExpect(status().isForbidden());
            postAs(member, "/api/usuarios/convites", invitation(uniqueEmail(), "ADVOGADO"))
                    .andExpect(status().isForbidden());
            deleteAs(member, "/api/usuarios/convites/" + UUID.randomUUID()).andExpect(status().isForbidden());
            patchAs(member, "/api/usuarios/papel", Map.of("userId", admin.userId(), "role", "ADVOGADO"))
                    .andExpect(status().isForbidden());
            deleteAs(member, "/api/usuarios/" + admin.userId()).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("select role from users where id = ?", String.class, admin.userId()))
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("inviting stores a pending invite for seven days with a lower-cased email")
    void invitesAUser() throws Exception {
        Account admin = registerAdmin();
        String email = uniqueEmail();

        JsonNode invite = bodyOf(postAs(admin, "/api/usuarios/convites",
                invitation(email.toUpperCase(), "SECRETARIA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Carla Nova"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("SECRETARIA"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty()));

        assertThat(invite.has("token")).isFalse();
        Map<String, Object> row = jdbc.queryForMap(
                "select organization_id, email, role, accepted_at, token, "
                        + "extract(epoch from (expires_at - created_at)) as seconds "
                        + "from user_invites where id = ?", invite.get("id").asText());
        assertThat(row.get("organization_id")).isEqualTo(admin.orgId());
        assertThat(row.get("email")).isEqualTo(email);
        assertThat(row.get("role")).isEqualTo("SECRETARIA");
        assertThat(row.get("accepted_at")).isNull();
        assertThat((String) row.get("token")).hasSize(32).matches("[0-9a-f]+");
        assertThat(((Number) row.get("seconds")).doubleValue()).isBetween(7 * 86400.0 - 5, 7 * 86400.0 + 5);
    }

    @Test
    @DisplayName("inviting queues the invitation email and records the event")
    void invitingSendsEmailAndEvent() throws Exception {
        Account admin = registerAdmin();
        String email = uniqueEmail();

        String id = bodyOf(postAs(admin, "/api/usuarios/convites", invitation(email, "ADVOGADO"))
                .andExpect(status().isCreated())).get("id").asText();

        String token = jdbc.queryForObject("select token from user_invites where id = ?", String.class, id);
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), message.capture());
        TarefaEmail task = (TarefaEmail) message.getValue();
        assertThat(task.tipo()).isEqualTo("CONVITE");
        assertThat(task.destinatario()).isEqualTo(email);
        assertThat(task.assunto()).isEqualTo("Convite para Escritorio Teste no Lexo");
        assertThat(task.corpo()).isEqualTo("Olá Carla Nova, você foi convidado para Escritorio Teste. "
                + "Acesse: http://localhost:8090/convite/" + token);

        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("CONVIDOU_USUARIO");
        assertThat(event.organizationId()).isEqualTo(admin.orgId());
        assertThat(event.entityType()).isEqualTo("USUARIO");
        assertThat(event.entityId()).isNull();
        assertThat(event.description()).isEqualTo("Convidou Carla Nova (" + email + ") como ADVOGADO");
        assertThat(event.userId()).isEqualTo(admin.userId());
        assertThat(event.userName()).isEqualTo("Ana Admin");
    }

    @Test
    @DisplayName("inviting still succeeds when the email queue is down")
    void invitingSurvivesQueueFailure() throws Exception {
        Account admin = registerAdmin();
        org.mockito.Mockito.doThrow(new IllegalStateException("rabbit down")).when(rabbit)
                .convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), org.mockito.ArgumentMatchers.<Object>any());

        postAs(admin, "/api/usuarios/convites", invitation(uniqueEmail(), "ADVOGADO"))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("select count(*) from user_invites where organization_id = ?",
                Integer.class, admin.orgId())).isEqualTo(1);
        assertThat(onlyPublishedEvent().type()).isEqualTo("CONVIDOU_USUARIO");
    }

    @Test
    @DisplayName("inviting an email that already has an account is a conflict")
    void invitingAnExistingUserConflicts() throws Exception {
        Account admin = registerAdmin();
        Account member = insertMember(admin, "ADVOGADO");

        postAs(admin, "/api/usuarios/convites", invitation(member.email().toUpperCase(), "ADVOGADO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLITO"))
                .andExpect(jsonPath("$.error").value("Já existe um usuário com este email"));
        verify(rabbit, never()).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY),
                org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("inviting an email with a pending invite is a conflict, until the invite expires")
    void invitingTwiceConflicts() throws Exception {
        Account admin = registerAdmin();
        String email = uniqueEmail();
        String first = inviteId(admin, email);

        postAs(admin, "/api/usuarios/convites", invitation(email.toUpperCase(), "ADVOGADO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLITO"))
                .andExpect(jsonPath("$.error").value("Já existe um convite pendente para este email"));

        expireInvite(first);
        postAs(admin, "/api/usuarios/convites", invitation(email, "ADVOGADO")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("another organization can invite the same email while its own invite is pending")
    void pendingInviteIsPerOrganization() throws Exception {
        Account first = registerAdmin();
        Account second = registerAdmin();
        String email = uniqueEmail();
        inviteId(first, email);

        postAs(second, "/api/usuarios/convites", invitation(email, "ADVOGADO")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("inviting validates the request")
    void invitingValidatesTheRequest() throws Exception {
        Account admin = registerAdmin();

        postAs(admin, "/api/usuarios/convites", Map.of("name", "C", "email", "invalido", "role", "ADVOGADO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO"));
        postAs(admin, "/api/usuarios/convites", Map.of("name", "Carla", "email", uniqueEmail()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO"));
        postAs(admin, "/api/usuarios/convites", Map.of("name", "Carla", "email", uniqueEmail(), "role", "DONO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Corpo da requisição inválido"));
    }

    @Test
    @DisplayName("pending invites list only unaccepted invites of the organization, newest first")
    void listsPendingInvites() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();
        String older = inviteId(admin, uniqueEmail());
        String newer = inviteId(admin, uniqueEmail());
        String accepted = inviteId(admin, uniqueEmail());
        inviteId(other, uniqueEmail());
        jdbc.update("update user_invites set created_at = now() - interval '2 hour' where id = ?", older);
        jdbc.update("update user_invites set created_at = now() - interval '1 hour' where id = ?", newer);
        jdbc.update("update user_invites set accepted_at = now() where id = ?", accepted);

        JsonNode invites = bodyOf(getAs(admin, "/api/usuarios/convites").andExpect(status().isOk()));

        assertThat(invites).hasSize(2);
        assertThat(invites.get(0).get("id").asText()).isEqualTo(newer);
        assertThat(invites.get(1).get("id").asText()).isEqualTo(older);
        assertThat(invites.get(0).has("token")).isFalse();
    }

    @Test
    @DisplayName("revoking an invite deletes it and records the event")
    void revokesAnInvite() throws Exception {
        Account admin = registerAdmin();
        String email = uniqueEmail();
        String id = inviteId(admin, email);
        org.mockito.Mockito.clearInvocations(events);

        deleteAs(admin, "/api/usuarios/convites/" + id).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from user_invites where id = ?", Integer.class, id)).isZero();
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("REVOGOU_CONVITE");
        assertThat(event.entityType()).isEqualTo("USUARIO");
        assertThat(event.entityId()).isNull();
        assertThat(event.description()).isEqualTo("Revogou convite de " + email);
        assertThat(event.userId()).isEqualTo(admin.userId());
    }

    @Test
    @DisplayName("revoking an unknown invite or one of another organization changes nothing")
    void revokingForeignInviteDoesNothing() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();
        String foreign = inviteId(other, uniqueEmail());
        org.mockito.Mockito.clearInvocations(events);

        deleteAs(admin, "/api/usuarios/convites/" + UUID.randomUUID()).andExpect(status().isNoContent());
        deleteAs(admin, "/api/usuarios/convites/" + foreign).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from user_invites where id = ?", Integer.class, foreign))
                .isEqualTo(1);
        assertThat(publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("changing a role updates the user, ends their sessions and records the event")
    void changesARole() throws Exception {
        Account admin = registerAdmin();
        Account member = insertMember(admin, "ADVOGADO");
        login(member.email(), PASSWORD, null).andExpect(status().isOk());
        assertThat(refreshTokensAreRevoked(member.userId())).isFalse();

        patchAs(admin, "/api/usuarios/papel", Map.of("userId", member.userId(), "role", "SECRETARIA"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select role from users where id = ?", String.class, member.userId()))
                .isEqualTo("SECRETARIA");
        assertThat(refreshTokensAreRevoked(member.userId())).isTrue();
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("ALTEROU_PAPEL");
        assertThat(event.entityType()).isEqualTo("USUARIO");
        assertThat(event.entityId()).isEqualTo(member.userId());
        assertThat(event.description()).isEqualTo("Alterou papel do usuário para SECRETARIA");
        assertThat(event.userId()).isEqualTo(admin.userId());
    }

    @Test
    @DisplayName("an admin cannot change their own role")
    void cannotChangeOwnRole() throws Exception {
        Account admin = registerAdmin();

        patchAs(admin, "/api/usuarios/papel", Map.of("userId", admin.userId(), "role", "ADVOGADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Você não pode alterar seu próprio papel"));
        assertThat(jdbc.queryForObject("select role from users where id = ?", String.class, admin.userId()))
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("changing the role of an unknown user or one of another organization is not found")
    void changingForeignRoleIsNotFound() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();

        patchAs(admin, "/api/usuarios/papel", Map.of("userId", UUID.randomUUID().toString(), "role", "ADVOGADO"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Usuário não encontrado"));
        patchAs(admin, "/api/usuarios/papel", Map.of("userId", other.userId(), "role", "ADVOGADO"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select role from users where id = ?", String.class, other.userId()))
                .isEqualTo("ADMIN");
        assertThat(publishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("changing a role validates the request")
    void changingRoleValidatesTheRequest() throws Exception {
        Account admin = registerAdmin();

        patchAs(admin, "/api/usuarios/papel", Map.of("userId", "", "role", "ADVOGADO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO"));
        patchAs(admin, "/api/usuarios/papel", Map.of("userId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("removing a user deletes the account, ends their sessions and records the event")
    void removesAUser() throws Exception {
        Account admin = registerAdmin();
        Account member = insertMember(admin, "ADVOGADO");
        login(member.email(), PASSWORD, null).andExpect(status().isOk());

        deleteAs(admin, "/api/usuarios/" + member.userId()).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from users where id = ?", Integer.class, member.userId()))
                .isZero();
        assertThat(refreshTokensAreRevoked(member.userId())).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ?", Integer.class,
                member.userId())).isEqualTo(1);
        DomainEvent event = onlyPublishedEvent();
        assertThat(event.type()).isEqualTo("REMOVEU_USUARIO");
        assertThat(event.entityType()).isEqualTo("USUARIO");
        assertThat(event.entityId()).isEqualTo(member.userId());
        assertThat(event.description()).isEqualTo("Removeu " + member.name() + " (" + member.email() + ")");
        login(member.email(), PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an admin cannot remove themselves")
    void cannotRemoveSelf() throws Exception {
        Account admin = registerAdmin();

        deleteAs(admin, "/api/usuarios/" + admin.userId()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.error").value("Você não pode remover a si mesmo"));
        assertThat(jdbc.queryForObject("select count(*) from users where id = ?", Integer.class, admin.userId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("removing an unknown user or one of another organization is not found")
    void removingForeignUserIsNotFound() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();

        deleteAs(admin, "/api/usuarios/" + UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuário não encontrado"));
        deleteAs(admin, "/api/usuarios/" + other.userId()).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from users where id = ?", Integer.class, other.userId()))
                .isEqualTo(1);
        assertThat(publishedEvents()).isEmpty();
    }
}
