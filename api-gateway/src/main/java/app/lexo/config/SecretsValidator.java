package app.lexo.config;

import app.lexo.common.signing.GatewaySignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
class SecretsValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SecretsValidator.class);

    static final String DEV_AUTH_SECRET = "troque-esta-chave-em-producao-com-no-minimo-32-bytes";

    private final String authSecret;
    private final String signingKey;
    private final Environment env;

    SecretsValidator(@Value("${lexo.auth-secret}") String authSecret,
                     @Value("${lexo.gateway.signing-key}") String signingKey, Environment env) {
        this.authSecret = authSecret;
        this.signingKey = signingKey;
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        var profiles = Arrays.asList(env.getActiveProfiles());
        boolean dev = profiles.contains("dev") || profiles.contains("test");
        GatewaySignature.exigirChaveSegura(signingKey, dev);
        checkAuthSecret(dev);
    }

    private void checkAuthSecret(boolean dev) {
        boolean insecure = authSecret == null || authSecret.isBlank() || DEV_AUTH_SECRET.equals(authSecret);
        if (!insecure) {
            return;
        }
        if (!dev) {
            throw new IllegalStateException(
                    "AUTH_SECRET inseguro ou ausente. Defina a variavel de ambiente AUTH_SECRET "
                    + "(>= 32 bytes), ou rode com o profile 'dev' para desenvolvimento local. "
                    + "O gateway recusa iniciar com o segredo default fora de dev.");
        }
        log.warn("*** AUTH_SECRET DEFAULT (profile dev). NUNCA use este segredo em producao. ***");
    }
}
