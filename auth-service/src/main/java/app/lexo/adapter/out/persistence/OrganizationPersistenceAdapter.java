package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.LoadOrganizationPort;
import app.lexo.application.port.out.SaveOrganizationPort;
import app.lexo.domain.Organization;
import app.lexo.domain.OrganizationId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class OrganizationPersistenceAdapter implements SaveOrganizationPort, LoadOrganizationPort {

    private final SpringDataOrganizationRepository repository;

    OrganizationPersistenceAdapter(SpringDataOrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Organization save(Organization organization) {
        OrganizationJpaEntity entity = organization.id() == null
                ? new OrganizationJpaEntity()
                : repository.findById(organization.id().value()).orElseThrow(() -> new IllegalStateException(
                        "Organization " + organization.id().value() + " does not exist to update"));
        entity.name = organization.name();
        entity.plan = organization.plan();
        entity.trialEndsAt = organization.trialEndsAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Organization> byId(OrganizationId id) {
        return repository.findById(id.value()).map(OrganizationPersistenceAdapter::toDomain);
    }

    private static Organization toDomain(OrganizationJpaEntity entity) {
        return Organization.restore(new OrganizationId(entity.getId()), entity.name, entity.plan,
                entity.trialEndsAt);
    }
}
