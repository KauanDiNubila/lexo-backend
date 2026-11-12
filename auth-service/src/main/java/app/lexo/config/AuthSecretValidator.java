package app.lexo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
class AuthSecretValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthSecretValidator.class);

    static final String DEFAULT_SECRET = "troque-esta-chave-em-producao-com-no-minimo-32-bytes";

    private final String secret;
    private final Environment env;

    AuthSecretValidator(@Value("${lexo.auth-secret}") String secret, Environment env) {
        this.secret = secret;
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        var profiles = Arrays.asList(env.getActiveProfiles());
        boolean dev = profiles.contains("dev") || profiles.contains("test");
        boolean insecure = secret == null || secret.isBlank() || DEFAULT_SECRET.equals(secret);
        if (!insecure) {
            return;
        }
        if (!dev) {
            throw new IllegalStateException(
                    "AUTH_SECRET inseguro ou ausente. Defina a variavel de ambiente AUTH_SECRET "
                    + "(>= 32 bytes), ou rode com o profile 'dev' para desenvolvimento local. "
                    + "O servico recusa iniciar com o segredo default fora de dev.");
        }
        log.warn("*** AUTH_SECRET DEFAULT (profile dev). NUNCA use este segredo em producao. ***");
    }
}
