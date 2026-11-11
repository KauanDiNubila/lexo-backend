package app.lexo.adapter.out.persistence;

import app.lexo.application.port.in.ClientOption;
import app.lexo.application.port.out.LoadClientPort;
import app.lexo.application.port.out.CountClientsPort;
import app.lexo.application.port.out.DeleteClientPort;
import app.lexo.application.port.out.ClientExistsPort;
import app.lexo.application.port.out.ListClientsPort;
import app.lexo.application.port.out.ListClientOptionsPort;
import app.lexo.application.port.out.SaveClientPort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageParamsMapper;
import app.lexo.common.web.PageResult;
import app.lexo.domain.Client;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class ClientPersistenceAdapter implements SaveClientPort, LoadClientPort, DeleteClientPort,
        ListClientsPort, ListClientOptionsPort, CountClientsPort, ClientExistsPort {

    private static final String OPTIONS_CACHE = "clientes-opcoes-v2";

    private final SpringDataClientRepository repository;

    ClientPersistenceAdapter(SpringDataClientRepository repository) {
        this.repository = repository;
    }

    @Override
    @CacheEvict(value = OPTIONS_CACHE, key = "#client.organizationId().value()")
    public Client save(Client client) {
        ClientJpaEntity entity = client.id() == null
                ? new ClientJpaEntity()
                : repository.findByIdAndOrganizationId(client.id().value(), client.organizationId().value()).orElseThrow(
                        () -> new IllegalStateException("Client " + client.id().value() + " does not exist to update"));
        ClientMapper.copyTo(client, entity);
        return ClientMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Client> byId(OrganizationId organizationId, ClientId clientId) {
        return repository.findByIdAndOrganizationId(clientId.value(), organizationId.value())
                .map(ClientMapper::toDomain);
    }

    @Override
    public Optional<Client> byPortalLink(String token) {
        return repository.findByPortalToken(token).map(ClientMapper::toDomain);
    }

    @Override
    @CacheEvict(value = OPTIONS_CACHE, key = "#client.organizationId().value()")
    public void delete(Client client) {
        repository.findByIdAndOrganizationId(client.id().value(), client.organizationId().value())
                .ifPresent(repository::delete);
    }

    @Override
    public PageResult<Client> list(OrganizationId organizationId, String search, PageParams pageParams) {
        Specification<ClientJpaEntity> filter =
                ClientSpecs.ofOrganization(organizationId.value()).and(ClientSpecs.search(search));
        return PageResult.of(repository.findAll(filter, PageParamsMapper.toPageRequest(pageParams)), ClientMapper::toDomain);
    }

    @Override
    @Cacheable(value = OPTIONS_CACHE, key = "#organizationId.value()")
    public List<ClientOption> options(OrganizationId organizationId) {
        return repository.findByOrganizationIdOrderByNameAsc(organizationId.value()).stream()
                .map(entity -> new ClientOption(entity.getId(), entity.name))
                .toList();
    }

    @Override
    public long count(OrganizationId organizationId) {
        return repository.countByOrganizationId(organizationId.value());
    }

    @Override
    public boolean exists(OrganizationId organizationId, ClientId clientId) {
        return repository.existsByIdAndOrganizationId(clientId.value(), organizationId.value());
    }
}
