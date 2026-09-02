package app.lexo.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * ADMIN inclui ADVOGADO, que inclui SECRETARIA — declarado uma vez so, em vez de
 * cada @PreAuthorize precisar enumerar "hasAnyRole('ADMIN', 'ADVOGADO')" e correr o
 * risco de esquecer um papel numa checagem nova.
 */
@Configuration
public class MethodSecurityConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_ADVOGADO
                ROLE_ADVOGADO > ROLE_SECRETARIA""");
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
