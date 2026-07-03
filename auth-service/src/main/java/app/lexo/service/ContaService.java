package app.lexo.service;

import app.lexo.controller.ApiException;
import app.lexo.domain.Organization;
import app.lexo.domain.User;
import app.lexo.dto.ContaDtos.ContaResponse;
import app.lexo.dto.ContaDtos.OrganizacaoInfo;
import app.lexo.dto.ContaDtos.PerfilInfo;
import app.lexo.repository.OrganizationRepository;
import app.lexo.repository.UserRepository;
import app.lexo.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaService {

    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;

    public ContaService(OrganizationRepository orgRepo, UserRepository userRepo) {
        this.orgRepo = orgRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public ContaResponse obterConta(AuthUser me) {
        Organization org = carregarOrg(me);
        User user = carregarUsuario(me);
        return new ContaResponse(infoDe(org, me.organizationId()), perfilDe(user));
    }

    @Transactional
    public OrganizacaoInfo atualizarOrganizacao(AuthUser me, String novoNome) {
        Organization org = carregarOrg(me);
        org.setName(novoNome.trim());
        orgRepo.save(org);
        return infoDe(org, me.organizationId());
    }

    @Transactional
    public PerfilInfo atualizarPerfil(AuthUser me, String novoNome) {
        User user = carregarUsuario(me);
        user.setName(novoNome.trim());
        userRepo.save(user);
        return perfilDe(user);
    }

    private Organization carregarOrg(AuthUser me) {
        return orgRepo.findById(me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Escritório não encontrado"));
    }

    private User carregarUsuario(AuthUser me) {
        return userRepo.findByIdAndOrganizationId(me.id(), me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
    }

    private OrganizacaoInfo infoDe(Organization org, String orgId) {
        int membros = userRepo.findByOrganizationIdOrderByNameAsc(orgId).size();
        return new OrganizacaoInfo(org.getId(), org.getName(), org.getPlan(), org.getTrialEndsAt(), membros);
    }

    private PerfilInfo perfilDe(User user) {
        return new PerfilInfo(user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.isTotpEnabled());
    }
}
