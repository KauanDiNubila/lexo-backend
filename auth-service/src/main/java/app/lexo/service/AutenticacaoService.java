package app.lexo.service;

import app.lexo.common.security.Role;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.ErrorCode;
import app.lexo.domain.Organization;
import app.lexo.domain.User;
import app.lexo.dto.AuthDtos;
import app.lexo.repository.OrganizationRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AutenticacaoService {

    private static final String HASH_DUMMY =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final int TENTATIVAS_POR_EMAIL = 10;
    private static final int TENTATIVAS_POR_IP = 30;
    private static final long JANELA_SEGUNDOS = 15 * 60;

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LimiteRequisicoesService rateLimit;
    private final CriptografiaService crypto;
    private final TotpProvider totp;

    public AutenticacaoService(UserRepository userRepo, OrganizationRepository orgRepo,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       LimiteRequisicoesService rateLimit, CriptografiaService crypto, TotpProvider totp) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimit = rateLimit;
        this.crypto = crypto;
        this.totp = totp;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Transactional
    public AuthDtos.AuthResponse registrar(AuthDtos.RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw ApiException.badRequest(ErrorCode.SENHAS_NAO_COINCIDEM, "As senhas não coincidem");
        }
        String email = normalizarEmail(req.email());
        if (userRepo.existsByEmail(email)) {
            throw ApiException.conflict(ErrorCode.EMAIL_EM_USO, "Já existe um usuário com este email");
        }

        Organization org = new Organization();
        org.setName(req.organizationName());
        org.setTrialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        org = orgRepo.save(org);

        User user = new User();
        user.setOrganizationId(org.getId());
        user.setName(req.name());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.ADMIN);
        user = userRepo.save(user);

        return new AuthDtos.AuthResponse(jwtService.issue(user), paraInfo(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req, String ip) {
        String email = normalizarEmail(req.email());
        String chaveEmail = "login:email:" + email;
        String chaveIp = ip == null ? null : "login:ip:" + ip;

        boolean dentroDoLimite = rateLimit.verificar(chaveEmail, TENTATIVAS_POR_EMAIL, JANELA_SEGUNDOS);
        if (chaveIp != null) {
            dentroDoLimite &= rateLimit.verificar(chaveIp, TENTATIVAS_POR_IP, JANELA_SEGUNDOS);
        }
        if (!dentroDoLimite) {
            throw ApiException.unauthorized(
                    ErrorCode.CREDENCIAIS_INVALIDAS, "Email ou senha inválidos");
        }

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), HASH_DUMMY);
            throw ApiException.unauthorized(
                    ErrorCode.CREDENCIAIS_INVALIDAS, "Email ou senha inválidos");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(
                    ErrorCode.CREDENCIAIS_INVALIDAS, "Email ou senha inválidos");
        }

        if (user.isTotpEnabled() && user.getTotpSecret() != null) {
            if (req.totpCode() == null || req.totpCode().isBlank()) {
                throw ApiException.unauthorized(
                        ErrorCode.TOTP_REQUIRED,
                        "Código de verificação em dois fatores obrigatório");
            }
            String secret = crypto.decifrar(user.getTotpSecret());
            if (!totp.verify(secret, req.totpCode())) {
                throw ApiException.unauthorized(
                        ErrorCode.TOTP_INVALID, "Código de verificação inválido");
            }
        }

        rateLimit.limpar(chaveEmail);
        if (chaveIp != null) {
            rateLimit.limpar(chaveIp);
        }

        return new AuthDtos.AuthResponse(jwtService.issue(user), paraInfo(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse paraSessao(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(
                        ErrorCode.REFRESH_INVALIDO, "Sessão inválida. Faça login novamente."));
        return new AuthDtos.AuthResponse(jwtService.issue(user), paraInfo(user));
    }

    private AuthDtos.UserInfo paraInfo(User u) {
        return new AuthDtos.UserInfo(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getOrganizationId());
    }
}
