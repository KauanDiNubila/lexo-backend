package app.lexo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Public paths")
class PublicPathsTest {

    @Test
    @DisplayName("the login, register, refresh, logout and accept-invite paths are public only when exact")
    void exactPaths() {
        assertThat(PublicPaths.isPublic("/api/auth/login")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/register")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/refresh")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/senha/esqueci")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/senha/redefinir")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/logout")).isTrue();
        assertThat(PublicPaths.isPublic("/api/convites/aceitar")).isTrue();
        assertThat(PublicPaths.isPublic("/api/auth/login/extra")).isFalse();
        assertThat(PublicPaths.isPublic("/api/convites/aceitar-tudo")).isFalse();
    }

    @Test
    @DisplayName("invite info, the client portal and the docs are public by prefix")
    void prefixes() {
        assertThat(PublicPaths.isPublic("/api/convites/info/abc")).isTrue();
        assertThat(PublicPaths.isPublic("/api/portal/token")).isTrue();
        assertThat(PublicPaths.isPublic("/docs/auth-service/v3/api-docs")).isTrue();
    }

    @Test
    @DisplayName("a path that only looks like a public one is private")
    void lookalikes() {
        assertThat(PublicPaths.isPublic("/api/convites/info")).isFalse();
        assertThat(PublicPaths.isPublic("/api/portalx/token")).isFalse();
        assertThat(PublicPaths.isPublic("/api/clientes")).isFalse();
        assertThat(PublicPaths.isPublic("/documentos/x")).isFalse();
    }
}
