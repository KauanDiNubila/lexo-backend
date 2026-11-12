package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Account API: organization and profile")
class AccountApiBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("the account shows the organization with its member count and the profile of the caller")
    void showsAccount() throws Exception {
        Account admin = registerAdmin();
        insertMember(admin, "ADVOGADO");
        insertMember(admin, "SECRETARIA");
        registerAdmin();

        getAs(admin, "/api/conta").andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacao.id").value(admin.orgId()))
                .andExpect(jsonPath("$.organizacao.name").value("Escritorio Teste"))
                .andExpect(jsonPath("$.organizacao.plan").value("trial"))
                .andExpect(jsonPath("$.organizacao.trialEndsAt").isNotEmpty())
                .andExpect(jsonPath("$.organizacao.membros").value(3))
                .andExpect(jsonPath("$.usuario.id").value(admin.userId()))
                .andExpect(jsonPath("$.usuario.name").value("Ana Admin"))
                .andExpect(jsonPath("$.usuario.email").value(admin.email()))
                .andExpect(jsonPath("$.usuario.role").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.totpEnabled").value(false));
    }

    @Test
    @DisplayName("the account shows two factor as enabled once it is on")
    void showsTwoFactorEnabled() throws Exception {
        Account admin = registerAdmin();
        enableTwoFactor(admin);

        getAs(admin, "/api/conta").andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.totpEnabled").value(true));
    }

    @Test
    @DisplayName("a missing organization is reported as not found")
    void organizationNotFound() throws Exception {
        Account ghost = ghost(UUID.randomUUID().toString());

        getAs(ghost, "/api/conta").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.error").value("Escritório não encontrado"));
    }

    @Test
    @DisplayName("a missing user is reported as not found")
    void userNotFound() throws Exception {
        Account admin = registerAdmin();
        Account ghost = ghost(admin.orgId());

        getAs(ghost, "/api/conta").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("an admin renames the organization and the name is trimmed")
    void adminRenamesOrganization() throws Exception {
        Account admin = registerAdmin();
        insertMember(admin, "ADVOGADO");

        putAs(admin, "/api/organizacao", Map.of("name", "  Novo Escritorio  ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.orgId()))
                .andExpect(jsonPath("$.name").value("Novo Escritorio"))
                .andExpect(jsonPath("$.plan").value("trial"))
                .andExpect(jsonPath("$.membros").value(2));

        assertThat(jdbc.queryForObject("select name from organizations where id = ?", String.class, admin.orgId()))
                .isEqualTo("Novo Escritorio");
    }

    @Test
    @DisplayName("only an admin can rename the organization")
    void nonAdminCannotRenameOrganization() throws Exception {
        Account admin = registerAdmin();
        Account lawyer = insertMember(admin, "ADVOGADO");

        putAs(lawyer, "/api/organizacao", Map.of("name", "Outro Nome")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));

        assertThat(jdbc.queryForObject("select name from organizations where id = ?", String.class, admin.orgId()))
                .isEqualTo("Escritorio Teste");
    }

    @Test
    @DisplayName("renaming the organization validates the name")
    void organizationNameIsValidated() throws Exception {
        Account admin = registerAdmin();

        putAs(admin, "/api/organizacao", Map.of("name", "A")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Nome do escritório muito curto"));
        putAs(admin, "/api/organizacao", Map.of("name", "")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"));
    }

    @Test
    @DisplayName("any user updates their own profile name, trimmed")
    void updatesProfile() throws Exception {
        Account admin = registerAdmin();
        Account lawyer = insertMember(admin, "ADVOGADO");

        putAs(lawyer, "/api/perfil", Map.of("name", "  Bruno Souza ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lawyer.userId()))
                .andExpect(jsonPath("$.name").value("Bruno Souza"))
                .andExpect(jsonPath("$.email").value(lawyer.email()))
                .andExpect(jsonPath("$.role").value("ADVOGADO"))
                .andExpect(jsonPath("$.totpEnabled").value(false));

        assertThat(jdbc.queryForObject("select name from users where id = ?", String.class, lawyer.userId()))
                .isEqualTo("Bruno Souza");
        assertThat(jdbc.queryForObject("select name from users where id = ?", String.class, admin.userId()))
                .isEqualTo("Ana Admin");
    }

    @Test
    @DisplayName("updating the profile validates the name")
    void profileNameIsValidated() throws Exception {
        Account admin = registerAdmin();

        putAs(admin, "/api/perfil", Map.of("name", "A")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error").value("Nome muito curto"));
    }

    @Test
    @DisplayName("the account endpoint needs authentication")
    void needsAuthentication() throws Exception {
        mvc.perform(get("/api/conta")).andExpect(status().isForbidden());
    }
}
