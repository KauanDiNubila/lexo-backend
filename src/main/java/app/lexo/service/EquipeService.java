package app.lexo.service;

import app.lexo.domain.User;
import app.lexo.domain.UserInvite;
import app.lexo.dto.TeamDtos.AcceptInviteRequest;
import app.lexo.dto.TeamDtos.InviteInfo;
import app.lexo.dto.TeamDtos.InviteRequest;
import app.lexo.dto.TeamDtos.InviteResponse;
import app.lexo.dto.TeamDtos.UpdateRoleRequest;
import app.lexo.dto.TeamDtos.UserResponse;
import app.lexo.repository.OrganizationRepository;
import app.lexo.repository.UserInviteRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.AuthUser;
import app.lexo.controller.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EquipeService {

    private final UserRepository userRepo;
    private final UserInviteRepository inviteRepo;
    private final OrganizationRepository orgRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService email;
    private final AuditoriaService audit;
    private final String baseUrl;

    public EquipeService(UserRepository userRepo, UserInviteRepository inviteRepo,
                       OrganizationRepository orgRepo, PasswordEncoder passwordEncoder,
                       EmailService email, AuditoriaService audit,
                       @Value("${lexo.base-url}") String baseUrl) {
        this.userRepo = userRepo;
        this.inviteRepo = inviteRepo;
        this.orgRepo = orgRepo;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.audit = audit;
        this.baseUrl = baseUrl;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listarUsuarios(AuthUser me) {
        return userRepo.findByOrganizationIdOrderByNameAsc(me.organizationId())
                .stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> listarConvitesPendentes(AuthUser me) {
        return inviteRepo.findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(me.organizationId())
                .stream().map(InviteResponse::from).toList();
    }

    @Transactional
    public InviteResponse convidar(AuthUser me, InviteRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw ApiException.conflict("Já existe um usuário com este email");
        }
        boolean pending = inviteRepo
                .findFirstByEmailAndOrganizationIdAndAcceptedAtIsNullAndExpiresAtAfter(
                        req.email(), me.organizationId(), Instant.now())
                .isPresent();
        if (pending) {
            throw ApiException.conflict("Já existe um convite pendente para este email");
        }

        UserInvite invite = new UserInvite();
        invite.setOrganizationId(me.organizationId());
        invite.setName(req.name());
        invite.setEmail(req.email());
        invite.setRole(req.role());
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invite = inviteRepo.save(invite);

        String orgName = orgRepo.findById(me.organizationId())
                .map(o -> o.getName()).orElse("Lexo");
        String acceptUrl = baseUrl + "/convite/" + invite.getToken();
        email.sendInvite(req.email(), orgName, req.name(), acceptUrl);

        audit.registrar(me.organizationId(), me.id(), nomeDe(me), "CONVIDOU_USUARIO", "USUARIO", null,
                "Convidou " + req.name() + " (" + req.email() + ") como " + req.role());

        return InviteResponse.from(invite);
    }

    @Transactional
    public void revogarConvite(AuthUser me, String inviteId) {
        UserInvite invite = inviteRepo.findByIdAndOrganizationId(inviteId, me.organizationId())
                .orElse(null);
        inviteRepo.deleteByIdAndOrganizationId(inviteId, me.organizationId());
        if (invite != null) {
            audit.registrar(me.organizationId(), me.id(), nomeDe(me), "REVOGOU_CONVITE", "USUARIO", null,
                    "Revogou convite de " + invite.getEmail());
        }
    }

    @Transactional
    public void alterarPapel(AuthUser me, UpdateRoleRequest req) {
        if (req.userId().equals(me.id())) {
            throw ApiException.badRequest("Você não pode alterar seu próprio papel");
        }
        User user = userRepo.findByIdAndOrganizationId(req.userId(), me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        user.setRole(req.role());
        userRepo.save(user);

        audit.registrar(me.organizationId(), me.id(), nomeDe(me), "ALTEROU_PAPEL", "USUARIO", req.userId(),
                "Alterou papel do usuário para " + req.role());
    }

    @Transactional
    public void removerUsuario(AuthUser me, String userId) {
        if (userId.equals(me.id())) {
            throw ApiException.badRequest("Você não pode remover a si mesmo");
        }
        User target = userRepo.findByIdAndOrganizationId(userId, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        userRepo.delete(target);

        audit.registrar(me.organizationId(), me.id(), nomeDe(me), "REMOVEU_USUARIO", "USUARIO", userId,
                "Removeu " + target.getName() + " (" + target.getEmail() + ")");
    }

    @Transactional(readOnly = true)
    public InviteInfo infoConvite(String token) {
        UserInvite invite = inviteRepo.findByToken(token).orElse(null);
        if (invite == null) {
            return new InviteInfo(null, null, null, false, "Convite inválido");
        }
        if (invite.getAcceptedAt() != null) {
            return new InviteInfo(null, invite.getName(), invite.getEmail(), false, "Este convite já foi utilizado");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            return new InviteInfo(null, invite.getName(), invite.getEmail(), false, "Este convite expirou");
        }
        String orgName = orgRepo.findById(invite.getOrganizationId())
                .map(o -> o.getName()).orElse("Lexo");
        return new InviteInfo(orgName, invite.getName(), invite.getEmail(), true, null);
    }

    @Transactional
    public void aceitarConvite(AcceptInviteRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw ApiException.badRequest("As senhas não coincidem");
        }
        UserInvite invite = inviteRepo.findByToken(req.token())
                .orElseThrow(() -> ApiException.badRequest("Convite inválido"));
        if (invite.getAcceptedAt() != null) {
            throw ApiException.badRequest("Este convite já foi utilizado");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Este convite expirou");
        }
        if (userRepo.existsByEmail(invite.getEmail())) {
            throw ApiException.conflict("Já existe uma conta com este email");
        }

        User user = new User();
        user.setOrganizationId(invite.getOrganizationId());
        user.setName(invite.getName());
        user.setEmail(invite.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(invite.getRole());
        userRepo.save(user);

        invite.setAcceptedAt(Instant.now());
        inviteRepo.save(invite);
    }

    private String nomeDe(AuthUser me) {
        if (me.name() != null) return me.name();
        if (me.email() != null) return me.email();
        return "Admin";
    }
}
