package app.lexo.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthSecretValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthSecretValidator.class);

    static final String SEGREDO_DEV = "troque-esta-chave-em-producao-com-no-minimo-32-bytes";

    private final String secret;
    private final Environment env;

    public AuthSecretValidator(@Value("${lexo.auth-secret}") String secret, Environment env) {
        this.secret = secret;
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        boolean producao = Arrays.asList(env.getActiveProfiles()).contains("prod");
        boolean inseguro = secret == null || secret.isBlank() || SEGREDO_DEV.equals(secret);
        if (!inseguro) {
            return;
        }
        if (producao) {
            throw new IllegalStateException(
                    "AUTH_SECRET nao configurado em producao. Defina a variavel de ambiente "
                    + "AUTH_SECRET (>= 32 bytes) — o gateway recusa iniciar com o segredo default.");
        }
        log.warn("*** Usando o AUTH_SECRET DEFAULT de desenvolvimento. NUNCA use em producao: "
                + "defina a env AUTH_SECRET (>= 32 bytes). ***");
    }
}
