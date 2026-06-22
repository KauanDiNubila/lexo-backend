package app.lexo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o Swagger/OpenAPI com esquema de seguranca Bearer (JWT).
 * Assim o botao "Authorize" aparece na UI: cole o token retornado por /api/auth/login
 * e todas as rotas protegidas passam a ser testaveis.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI lexoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lexo API")
                        .version("0.1.0")
                        .description("API do backend do Lexo — gestao para escritorios de advocacia. "
                                + "Faca login em POST /api/auth/login, copie o token e use o botao "
                                + "Authorize para testar as rotas protegidas."))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
