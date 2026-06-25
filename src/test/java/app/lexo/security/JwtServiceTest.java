package app.lexo.security;

import app.lexo.domain.User;
import app.lexo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Emissao e leitura de JWT")
class JwtServiceTest {

    private final JwtService jwt = new JwtService("segredo-de-teste-com-no-minimo-32-bytes-aqui", 720);

    private User usuarioExemplo() {
        User u = new User();
        u.setId("user-1");
        u.setOrganizationId("org-1");
        u.setName("Ana");
        u.setEmail("ana@exemplo.com");
        u.setRole(Role.ADMIN);
        return u;
    }

    @Test
    @DisplayName("token emitido e relido preserva os dados do usuario")
    void roundtrip() {
        String token = jwt.issue(usuarioExemplo());
        AuthUser lido = jwt.parse(token);

        assertEquals("user-1", lido.id());
        assertEquals("org-1", lido.organizationId());
        assertEquals("ana@exemplo.com", lido.email());
        assertEquals(Role.ADMIN, lido.role());
    }

    @Test
    @DisplayName("token invalido/adulterado e rejeitado")
    void tokenInvalido() {
        assertThrows(Exception.class, () -> jwt.parse("token.invalido.qualquer"));
    }

    @Test
    @DisplayName("segredo curto (< 32 bytes) e recusado na construcao")
    void segredoCurto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto", 720));
    }
}
