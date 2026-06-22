package app.lexo.service;

import app.lexo.domain.Client;
import app.lexo.dto.ClientDtos.ClientRequest;
import app.lexo.dto.ClientDtos.ClientResponse;
import app.lexo.repository.ClientRepository;
import app.lexo.security.AuthUser;
import app.lexo.util.DocumentValidator;
import app.lexo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository repo;

    public ClientService(ClientRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> list(AuthUser me) {
        return repo.findByOrganizationIdOrderByNameAsc(me.organizationId())
                .stream().map(ClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse get(AuthUser me, String id) {
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        return ClientResponse.from(c);
    }

    @Transactional
    public ClientResponse create(AuthUser me, ClientRequest req) {
        validateDocument(req.document());
        Client c = new Client();
        c.setOrganizationId(me.organizationId());
        apply(c, req);
        return ClientResponse.from(repo.save(c));
    }

    @Transactional
    public ClientResponse update(AuthUser me, String id, ClientRequest req) {
        validateDocument(req.document());
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        apply(c, req);
        return ClientResponse.from(repo.save(c));
    }

    @Transactional
    public void delete(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private void validateDocument(String document) {
        if (document != null && !document.isBlank() && !DocumentValidator.isValid(document)) {
            throw ApiException.badRequest("CPF ou CNPJ inválido");
        }
    }

    private void apply(Client c, ClientRequest req) {
        c.setName(req.name());
        c.setDocument(blankToNull(req.document()));
        c.setEmail(blankToNull(req.email()));
        c.setPhone(blankToNull(req.phone()));
        c.setNotes(blankToNull(req.notes()));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
