package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.DeleteInvitePort;
import app.lexo.application.port.out.ListPendingInvitesPort;
import app.lexo.application.port.out.LoadInvitePort;
import app.lexo.application.port.out.PendingInviteExistsPort;
import app.lexo.application.port.out.SaveInvitePort;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.InviteId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserInvite;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class InvitePersistenceAdapter implements SaveInvitePort, LoadInvitePort, ListPendingInvitesPort,
        PendingInviteExistsPort, DeleteInvitePort {

    private final SpringDataInviteRepository repository;

    InvitePersistenceAdapter(SpringDataInviteRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserInvite save(UserInvite invite) {
        InviteJpaEntity entity = invite.id() == null
                ? new InviteJpaEntity()
                : repository.findById(invite.id().value()).orElseThrow(
                        () -> new IllegalStateException("Invite " + invite.id().value() + " does not exist to update"));
        entity.organizationId = invite.organizationId().value();
        entity.name = invite.name();
        entity.email = invite.email().value();
        entity.role = invite.role();
        entity.token = invite.token();
        entity.expiresAt = invite.expiresAt();
        entity.acceptedAt = invite.acceptedAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<UserInvite> byToken(String token) {
        return repository.findByToken(token).map(InvitePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<UserInvite> inOrganization(OrganizationId organizationId, InviteId id) {
        return repository.findByIdAndOrganizationId(id.value(), organizationId.value())
                .map(InvitePersistenceAdapter::toDomain);
    }

    @Override
    public List<UserInvite> of(OrganizationId organizationId) {
        return repository.findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(organizationId.value())
                .stream().map(InvitePersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean exists(EmailAddress email, OrganizationId organizationId, Instant now) {
        return repository.existsByEmailAndOrganizationIdAndAcceptedAtIsNullAndExpiresAtAfter(email.value(),
                organizationId.value(), now);
    }

    @Override
    public void delete(OrganizationId organizationId, InviteId id) {
        repository.deleteByIdAndOrganizationId(id.value(), organizationId.value());
    }

    private static UserInvite toDomain(InviteJpaEntity entity) {
        return UserInvite.restore(new InviteId(entity.getId()), new OrganizationId(entity.organizationId),
                entity.name, new EmailAddress(entity.email), entity.role, entity.token, entity.expiresAt,
                entity.acceptedAt, entity.getCreatedAt());
    }
}
