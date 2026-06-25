package app.lexo.security;

import app.lexo.domain.enums.Role;

/**
 * Principal autenticado, extraido do JWT. Carrega o tenant (organizationId) e o papel,
 * usados em todas as verificacoes de autorizacao no ponto de uso.
 */
public record AuthUser(
        String id,
        String organizationId,
        String name,
        String email,
        Role role
) {
}
