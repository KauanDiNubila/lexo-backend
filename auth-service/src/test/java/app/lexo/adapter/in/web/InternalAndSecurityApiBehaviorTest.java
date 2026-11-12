package app.lexo.adapter.in.web;

import app.lexo.IntegrationTestBase;
import app.lexo.common.signing.GatewaySignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal and security behavior")
class InternalAndSecurityApiBehaviorTest extends IntegrationTestBase {

    private static final String INTERNAL_HEADER = "X-Internal-Key";

    @Test
    @DisplayName("the internal user check answers true only for a user of that organization")
    void internalExists() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();

        mvc.perform(get("/internal/usuarios/{id}/existe", admin.userId()).param("orgId", admin.orgId())
                .header(INTERNAL_HEADER, INTERNAL_KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/internal/usuarios/{id}/existe", admin.userId()).param("orgId", other.orgId())
                .header(INTERNAL_HEADER, INTERNAL_KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get("/internal/usuarios/{id}/existe", UUID.randomUUID().toString()).param("orgId", admin.orgId())
                .header(INTERNAL_HEADER, INTERNAL_KEY)).andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    @DisplayName("the internal user lookup returns the contact data, scoped to the organization")
    void internalLookup() throws Exception {
        Account admin = registerAdmin();
        Account other = registerAdmin();

        mvc.perform(get("/internal/usuarios/{id}", admin.userId()).param("orgId", admin.orgId())
                .header(INTERNAL_HEADER, INTERNAL_KEY)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.userId()))
                .andExpect(jsonPath("$.name").value("Ana Admin"))
                .andExpect(jsonPath("$.email").value(admin.email()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/internal/usuarios/{id}", admin.userId()).param("orgId", other.orgId())
                .header(INTERNAL_HEADER, INTERNAL_KEY)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.error").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("internal endpoints refuse calls without the internal key or with a wrong one")
    void internalNeedsTheKey() throws Exception {
        Account admin = registerAdmin();

        int withoutKey = mvc.perform(get("/internal/usuarios/{id}/existe", admin.userId())
                .param("orgId", admin.orgId())).andReturn().getResponse().getStatus();
        int wrongKey = mvc.perform(get("/internal/usuarios/{id}/existe", admin.userId())
                .param("orgId", admin.orgId()).header(INTERNAL_HEADER, "chave-errada"))
                .andReturn().getResponse().getStatus();
        int gatewayCaller = mvc.perform(get("/internal/usuarios/{id}/existe", admin.userId())
                .param("orgId", admin.orgId()).headers(asGateway(admin))).andReturn().getResponse().getStatus();

        assertThat(withoutKey).isIn(401, 403);
        assertThat(wrongKey).isIn(401, 403);
        assertThat(gatewayCaller).isIn(401, 403);
    }

    @Test
    @DisplayName("protected endpoints refuse calls without gateway headers")
    void protectedNeedsGatewayHeaders() throws Exception {
        assertThat(mvc.perform(get("/api/conta")).andReturn().getResponse().getStatus()).isIn(401, 403);
        assertThat(mvc.perform(get("/api/usuarios")).andReturn().getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    @DisplayName("protected endpoints refuse unsigned, wrongly signed and stale gateway headers")
    void protectedNeedsAValidSignature() throws Exception {
        Account admin = registerAdmin();
        HttpHeaders unsigned = unsigned(admin);
        HttpHeaders wrongKey = signedWith(admin, "outra-chave-de-assinatura-com-mais-de-32-bytes",
                Instant.now().getEpochSecond());
        HttpHeaders stale = signedWith(admin, SIGNING_KEY, Instant.now().minusSeconds(3600).getEpochSecond());
        HttpHeaders tampered = asGateway(admin);
        tampered.set("X-User-Role", "ADMIN");
        tampered.set("X-Org-Id", UUID.randomUUID().toString());

        for (HttpHeaders headers : new HttpHeaders[] {unsigned, wrongKey, stale, tampered}) {
            int result = mvc.perform(get("/api/conta").headers(headers)).andReturn().getResponse().getStatus();
            assertThat(result).isIn(401, 403);
        }
        mvc.perform(get("/api/conta").headers(asGateway(admin))).andExpect(status().isOk());
        assertThat(GatewaySignature.H_TIMESTAMP).isNotBlank();
    }

    @Test
    @DisplayName("swagger and the invite info are public")
    void publicPaths() throws Exception {
        assertThat(mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus()).isNotIn(401, 403);
        assertThat(mvc.perform(get("/api/convites/info/x")).andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("CORS answers the configured origin with credentials")
    void cors() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                .header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Credentials", "true"));
        int foreign = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .options("/api/auth/login").header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "POST")).andReturn().getResponse().getStatus();
        assertThat(foreign).isEqualTo(403);
    }
}
