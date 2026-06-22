package app.lexo.service;

import app.lexo.domain.Organization;
import app.lexo.domain.User;
import app.lexo.domain.enums.Role;
import app.lexo.dto.AuthDtos;
import app.lexo.repository.OrganizationRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.JwtService;
import app.lexo.controller.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitService rateLimit;
    private final CryptoService crypto;
    private final TotpProvider totp;

    public AuthService(UserRepository userRepo, OrganizationRepository orgRepo,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       RateLimitService rateLimit, CryptoService crypto, TotpProvider totp) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimit = rateLimit;
        this.crypto = crypto;
        this.totp = totp;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw ApiException.badRequest("As senhas não coincidem");
        }
        if (userRepo.existsByEmail(req.email())) {
            throw ApiException.conflict("Já existe um usuário com este email");
        }

        Organization org = new Organization();
        org.setName(req.organizationName());
        org.setTrialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        org = orgRepo.save(org);

        User user = new User();
        user.setOrganizationId(org.getId());
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.ADMIN);
        user = userRepo.save(user);

        return new AuthDtos.AuthResponse(jwtService.issue(user), toInfo(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        // Trava brute force/credential stuffing por email: 10 tentativas / 15 min.
        // Retorna o mesmo erro de credencial invalida para nao revelar o bloqueio.
        String key = "login:" + req.email().toLowerCase();
        if (!rateLimit.check(key, 10, 15 * 60)) {
            throw ApiException.unauthorized("Email ou senha inválidos");
        }

        User user = userRepo.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email ou senha inválidos");
        }

        if (user.isTotpEnabled() && user.getTotpSecret() != null) {
            if (req.totpCode() == null || req.totpCode().isBlank()) {
                throw ApiException.unauthorized("Código de verificação em dois fatores obrigatório");
            }
            String secret = crypto.decrypt(user.getTotpSecret());
            if (!totp.verify(secret, req.totpCode())) {
                throw ApiException.unauthorized("Código de verificação inválido");
            }
        }

        return new AuthDtos.AuthResponse(jwtService.issue(user), toInfo(user));
    }

    private AuthDtos.UserInfo toInfo(User u) {
        return new AuthDtos.UserInfo(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getOrganizationId());
    }
}
