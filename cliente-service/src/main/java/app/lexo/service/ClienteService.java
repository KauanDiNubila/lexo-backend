package app.lexo.service;

import app.lexo.common.evento.EventoDominio;
import app.lexo.common.security.AuthUser;
import app.lexo.common.web.ApiException;
import app.lexo.common.web.Pagina;
import app.lexo.domain.Client;
import app.lexo.dto.ClientDtos.ClientRequest;
import app.lexo.dto.ClientDtos.ClientResponse;
import app.lexo.dto.ClientDtos.ClienteOpcao;
import app.lexo.dto.ClientDtos.ResumoClientes;
import app.lexo.evento.PublicadorEventos;
import app.lexo.repository.ClientRepository;
import app.lexo.repository.ClientSpecs;
import app.lexo.util.DocumentValidator;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private static final String CACHE_OPCOES = "clientes-opcoes";

    private final ClientRepository repo;
    private final PublicadorEventos eventos;

    public ClienteService(ClientRepository repo, PublicadorEventos eventos) {
        this.repo = repo;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Pagina<ClientResponse> listar(AuthUser me, String busca, Pageable pageable) {
        var filtro = ClientSpecs.daOrganizacao(me.organizationId()).and(ClientSpecs.busca(busca));
        return Pagina.de(repo.findAll(filtro, pageable), ClientResponse::from);
    }

    @Cacheable(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional(readOnly = true)
    public List<ClienteOpcao> opcoes(AuthUser me) {
        return repo.findByOrganizationIdOrderByNameAsc(me.organizationId())
                .stream().map(ClienteOpcao::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumoClientes resumo(AuthUser me) {
        return new ResumoClientes(repo.countByOrganizationId(me.organizationId()));
    }

    @Transactional(readOnly = true)
    public ClientResponse buscar(AuthUser me, String id) {
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        return ClientResponse.from(c);
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public ClientResponse criar(AuthUser me, ClientRequest req) {
        validarDocumento(req.document());
        Client c = new Client();
        c.setOrganizationId(me.organizationId());
        preencher(c, req);
        c = repo.save(c);

        eventos.publicar(EventoDominio.de("CLIENTE_CRIADO", me.organizationId(), "CLIENTE",
                c.getId(), "Cliente criado: " + c.getName(), me.id(), nomeDe(me)));
        return ClientResponse.from(c);
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public ClientResponse atualizar(AuthUser me, String id, ClientRequest req) {
        validarDocumento(req.document());
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        preencher(c, req);
        return ClientResponse.from(repo.save(c));
    }

    @CacheEvict(value = CACHE_OPCOES, key = "#me.organizationId()")
    @Transactional
    public void excluir(AuthUser me, String id) {
        Client c = repo.findByIdAndOrganizationId(id, me.organizationId())
                .orElseThrow(() -> ApiException.notFound("Cliente não encontrado"));
        repo.delete(c);

        eventos.publicar(EventoDominio.de("CLIENTE_EXCLUIDO", me.organizationId(), "CLIENTE",
                c.getId(), "Cliente excluído: " + c.getName(), me.id(), nomeDe(me)));
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

    private String nomeDe(AuthUser me) {
        if (me.name() != null) return me.name();
        return me.email() != null ? me.email() : "Sistema";
    }
}
