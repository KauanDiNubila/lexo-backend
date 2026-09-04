package app.lexo.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(OpenAPI.class)
public class LexoOpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI lexoOpenAPI(
            @Value("${spring.application.name}") String servico,
            @Value("${lexo.public-api-url}") String urlPublica) {
        return new OpenAPI()
                .info(new Info()
                        .title("Lexo API — " + servico)
                        .version("0.1.0")
                        .description("Documentacao gerada a partir dos controllers do " + servico + ". "
                                + "Faca login em POST /api/auth/login, copie o accessToken e use o botao "
                                + "Authorize para testar as rotas protegidas atraves do gateway."))
                .servers(List.of(new Server().url(urlPublica)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
