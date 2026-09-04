package app.lexo.service;

import app.lexo.common.evento.EventoDominio;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.ApiException;
import app.lexo.domain.User;
import app.lexo.dto.TotpDtos.InitiateResponse;
import app.lexo.common.kafka.PublicadorEventos;
import app.lexo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoisFatoresService {

    private final UserRepository userRepo;
    private final TotpProvider totp;
    private final CriptografiaService crypto;
    private final PublicadorEventos eventos;

    public DoisFatoresService(UserRepository userRepo, TotpProvider totp,
                       CriptografiaService crypto, PublicadorEventos eventos) {
        this.userRepo = userRepo;
        this.totp = totp;
        this.crypto = crypto;
        this.eventos = eventos;
    }

    @Transactional
    public InitiateResponse iniciar(AuthUser me) {
        User user = usuarioAtual(me);
        String secret = totp.generateSecret();

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

        user.setTotpSecret(user.getTotpPendingSecret());
        user.setTotpEnabled(true);
        user.setTotpPendingSecret(null);
        userRepo.save(user);

        eventos.publicar(EventoDominio.de("ATIVOU_2FA", me.organizationId(), null, null,
                "Ativou verificação em dois fatores", me.id(), nomeDe(me)));
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

        eventos.publicar(EventoDominio.de("DESATIVOU_2FA", me.organizationId(), null, null,
                "Desativou verificação em dois fatores", me.id(), nomeDe(me)));
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
