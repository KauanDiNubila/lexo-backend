package app.lexo.service;

import app.lexo.domain.User;
import app.lexo.dto.TotpDtos.InitiateResponse;
import app.lexo.repository.UserRepository;
import app.lexo.security.AuthUser;
import app.lexo.controller.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestao do 2FA (TOTP), portada de actions/totp.ts. Segredos cifrados em repouso. */
@Service
public class DoisFatoresService {

    private final UserRepository userRepo;
    private final TotpProvider totp;
    private final CriptografiaService crypto;
    private final AuditoriaService audit;

    public DoisFatoresService(UserRepository userRepo, TotpProvider totp,
                       CriptografiaService crypto, AuditoriaService audit) {
        this.userRepo = userRepo;
        this.totp = totp;
        this.crypto = crypto;
        this.audit = audit;
    }

    @Transactional
    public InitiateResponse iniciar(AuthUser me) {
        User user = usuarioAtual(me);
        String secret = totp.generateSecret();
        // O segredo pendente vai cifrado ao banco.
        user.setTotpPendingSecret(crypto.cifrar(secret));
        userRepo.save(user);
        return new InitiateResponse(secret, totp.otpauthUri(secret, user.getEmail()));
    }

    @Transactional
    public void confirmar(AuthUser me, String code) {
        User user = usuarioAtual(me);
        if (user.getTotpPendingSecret() == null) {
            throw ApiException.badRequest("Sessão expirada. Reinicie o processo.");
        }
        String secret = crypto.decifrar(user.getTotpPendingSecret());
        if (!totp.verify(secret, code)) {
            throw ApiException.badRequest("Código inválido. Tente novamente.");
        }
        // O pendente ja esta cifrado; promove-se direto a totpSecret.
        user.setTotpSecret(user.getTotpPendingSecret());
        user.setTotpEnabled(true);
        user.setTotpPendingSecret(null);
        userRepo.save(user);

        audit.registrar(me.organizationId(), me.id(), nomeDe(me), "ATIVOU_2FA",
                "Ativou verificação em dois fatores");
    }

    @Transactional
    public void desativar(AuthUser me, String code) {
        User user = usuarioAtual(me);
        if (user.getTotpSecret() == null) {
            throw ApiException.badRequest("2FA não está ativado.");
        }
        if (!totp.verify(crypto.decifrar(user.getTotpSecret()), code)) {
            throw ApiException.badRequest("Código inválido.");
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        user.setTotpPendingSecret(null);
        userRepo.save(user);

        audit.registrar(me.organizationId(), me.id(), nomeDe(me), "DESATIVOU_2FA",
                "Desativou verificação em dois fatores");
    }

    private User usuarioAtual(AuthUser me) {
        return userRepo.findByIdAndOrganizationId(me.id(), me.organizationId())
                .orElseThrow(() -> ApiException.unauthorized("Usuário não encontrado"));
    }

    private String nomeDe(AuthUser me) {
        if (me.name() != null) return me.name();
        if (me.email() != null) return me.email();
        return "";
    }
}
