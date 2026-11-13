package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Audit API: behavior pinned before the hexagonal migration")
class AuditApiBehaviorTest extends IntegrationTestBase {

    @Test
    @DisplayName("an admin lists the organization's entries, newest first, with exactly the public fields")
    void listsNewestFirst() throws Exception {
        Identity admin = Identity.create();
        Instant now = Instant.now();
        insertEntry(admin.orgId(), "CLIENTE_CRIADO", "primeiro", now.minusSeconds(180));
        insertEntry(admin.orgId(), "PROCESSO_CRIADO", "segundo", now.minusSeconds(120));
        insertEntry(admin.orgId(), "CLIENTE_EXCLUIDO", "terceiro", now.minusSeconds(60));

        getAs(admin, "/api/auditoria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].description").value("terceiro"))
                .andExpect(jsonPath("$[1].description").value("segundo"))
                .andExpect(jsonPath("$[2].description").value("primeiro"))
                .andExpect(jsonPath("$[0].action").value("CLIENTE_EXCLUIDO"))
                .andExpect(jsonPath("$[0].userName").value("Ana Advogada"))
                .andExpect(jsonPath("$[0].entityType").value("CLIENTE"))
                .andExpect(jsonPath("$[0].entityId").value("c-1"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].organizationId").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    @DisplayName("the list is empty when the organization has no entries, and never shows another organization's")
    void isolatedByOrganization() throws Exception {
        Identity admin = Identity.create();
        Identity other = Identity.create();
        insertEntry(other.orgId(), "CLIENTE_CRIADO", "de outro escritorio", Instant.now());

        getAs(admin, "/api/auditoria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        getAs(other, "/api/auditoria")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("de outro escritorio"));
    }

    @Test
    @DisplayName("an entry without entity keeps the entity fields null")
    void entryWithoutEntity() throws Exception {
        Identity admin = Identity.create();
        jdbc.update("insert into audit_logs (id, organization_id, user_name, action, description, created_at) "
                        + "values (?, ?, 'Sistema', 'ALTEROU_PAPEL', 'sem entidade', ?)",
                UUID.randomUUID().toString(), admin.orgId(), Timestamp.from(Instant.now()));

        getAs(admin, "/api/auditoria")
                .andExpect(jsonPath("$[0].entityType").value(nullValue()))
                .andExpect(jsonPath("$[0].entityId").value(nullValue()));
    }

    @Test
    @DisplayName("only the 200 most recent entries are returned")
    void returnsAtMost200() throws Exception {
        Identity admin = Identity.create();
        Instant base = Instant.now().minusSeconds(3600);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 205; i++) {
            Timestamp at = Timestamp.from(base.plusSeconds(i));
            rows.add(new Object[]{UUID.randomUUID().toString(), admin.orgId(), "ACAO", "e" + i, at, at});
        }
        jdbc.batchUpdate("insert into audit_logs (id, organization_id, user_name, action, description, created_at, "
                + "updated_at) values (?, ?, 'Ana', ?, ?, ?, ?)", rows);

        getAs(admin, "/api/auditoria")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(200)))
                .andExpect(jsonPath("$[0].description").value("e204"))
                .andExpect(jsonPath("$[199].description").value("e5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADVOGADO", "SECRETARIA"})
    @DisplayName("anyone who is not an admin gets 403 ACESSO_NEGADO")
    void onlyAdmin(String role) throws Exception {
        Identity user = Identity.create(role);
        insertEntry(user.orgId(), "CLIENTE_CRIADO", "x", Instant.now());

        getAs(user, "/api/auditoria")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.error").value("Acesso negado"));
    }

    @Test
    @DisplayName("without gateway headers the answer is 403")
    void unauthenticated() throws Exception {
        mvc.perform(get("/api/auditoria"))
                .andExpect(status().isForbidden());
    }
}
