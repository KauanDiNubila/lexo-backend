package app.lexo.service;

import app.lexo.domain.User;
import app.lexo.dto.TotpDtos.InitiateResponse;
import app.lexo.repository.UserRepository;
import app.lexo.security.AuthUser;
import app.lexo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestao do 2FA (TOTP), portada de actions/totp.ts. Segredos cifrados em repouso. */
@Service
public class TotpService {

    private final UserRepository userRepo;
    private final TotpProvider totp;
    private final CryptoService crypto;
    private final AuditService audit;

    public TotpService(UserRepository userRepo, TotpProvider totp,
                       CryptoService crypto, AuditService audit) {
        this.userRepo = userRepo;
        this.totp = totp;
        this.crypto = crypto;
        this.audit = audit;
    }

    @Transactional
    public InitiateResponse initiate(AuthUser me) {
        User user = currentUser(me);
        String secret = totp.generateSecret();
        // O segredo pendente vai cifrado ao banco.
        user.setTotpPendingSecret(crypto.encrypt(secret));
        userRepo.save(user);
        return new InitiateResponse(secret, totp.otpauthUri(secret, user.getEmail()));
    }

    @Transactional
    public void confirm(AuthUser me, String code) {
        User user = currentUser(me);
        if (user.getTotpPendingSecret() == null) {
            throw ApiException.badRequest("Sessão expirada. Reinicie o processo.");
        }
        String secret = crypto.decrypt(user.getTotpPendingSecret());
        if (!totp.verify(secret, code)) {
            throw ApiException.badRequest("Código inválido. Tente novamente.");
        }
        // O pendente ja esta cifrado; promove-se direto a totpSecret.
        user.setTotpSecret(user.getTotpPendingSecret());
        user.setTotpEnabled(true);
        user.setTotpPendingSecret(null);
        userRepo.save(user);

        audit.log(me.organizationId(), me.id(), nameOf(me), "ATIVOU_2FA",
                "Ativou verificação em dois fatores");
    }

    @Transactional
    public void disable(AuthUser me, String code) {
        User user = currentUser(me);
        if (user.getTotpSecret() == null) {
            throw ApiException.badRequest("2FA não está ativado.");
        }
        if (!totp.verify(crypto.decrypt(user.getTotpSecret()), code)) {
            throw ApiException.badRequest("Código inválido.");
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        user.setTotpPendingSecret(null);
        userRepo.save(user);

        audit.log(me.organizationId(), me.id(), nameOf(me), "DESATIVOU_2FA",
                "Desativou verificação em dois fatores");
    }

    private User currentUser(AuthUser me) {
        return userRepo.findByIdAndOrganizationId(me.id(), me.organizationId())
                .orElseThrow(() -> ApiException.unauthorized("Usuário não encontrado"));
    }

    private String nameOf(AuthUser me) {
        if (me.name() != null) return me.name();
        if (me.email() != null) return me.email();
        return "";
    }
}
