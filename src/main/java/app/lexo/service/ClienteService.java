package app.lexo.service;

import app.lexo.domain.Client;
import app.lexo.dto.ClientDtos.ClientRequest;
import app.lexo.dto.ClientDtos.ClientResponse;
import app.lexo.repository.ClientRepository;
import app.lexo.security.AuthUser;
import app.lexo.util.DocumentValidator;
import app.lexo.controller.ApiException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    /** Nome do cache; a chave e o organizationId para isolar por tenant. */
    private static final String CACHE = "clientes";

    private final ClientRepository repo;

    public ClienteService(ClientRepository repo) {
        this.repo = repo;
    }

    @Cacheable(value = CACHE, key = "#me.organizationId()")
    @Transactional(readOnly = true)
    public List<ClientResponse> listar(AuthUser me) {
        return repo.findByOrganizationIdOrderByNameAsc(me.organizationId())
                .stream().map(ClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse buscar(AuthUser me, String id) {
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        return ClientResponse.from(c);
    }

    @CacheEvict(value = CACHE, key = "#me.organizationId()")
    @Transactional
    public ClientResponse criar(AuthUser me, ClientRequest req) {
        validarDocumento(req.document());
        Client c = new Client();
        c.setOrganizationId(me.organizationId());
        preencher(c, req);
        return ClientResponse.from(repo.save(c));
    }

    @CacheEvict(value = CACHE, key = "#me.organizationId()")
    @Transactional
    public ClientResponse atualizar(AuthUser me, String id, ClientRequest req) {
        validarDocumento(req.document());
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        preencher(c, req);
        return ClientResponse.from(repo.save(c));
    }

    @CacheEvict(value = CACHE, key = "#me.organizationId()")
    @Transactional
    public void excluir(AuthUser me, String id) {
        repo.deleteByIdAndOrganizationId(id, me.organizationId());
    }

    private void validarDocumento(String document) {
        if (document != null && !document.isBlank() && !DocumentValidator.isValid(document)) {
            throw ApiException.badRequest("CPF ou CNPJ inválido");
        }
    }

    private void preencher(Client c, ClientRequest req) {
        c.setName(req.name());
        c.setDocument(vazioParaNulo(req.document()));
        c.setEmail(vazioParaNulo(req.email()));
        c.setPhone(vazioParaNulo(req.phone()));
        c.setNotes(vazioParaNulo(req.notes()));
    }

    private String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
