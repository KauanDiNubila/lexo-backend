package app.lexo.adapter.out.persistence;

import app.lexo.application.port.out.CountUsersPort;
import app.lexo.application.port.out.DeleteUserPort;
import app.lexo.application.port.out.ListUsersPort;
import app.lexo.application.port.out.LoadUserPort;
import app.lexo.application.port.out.SaveUserPort;
import app.lexo.application.port.out.UserExistsPort;
import app.lexo.domain.EmailAddress;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.User;
import app.lexo.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class UserPersistenceAdapter implements SaveUserPort, LoadUserPort, UserExistsPort, ListUsersPort,
        CountUsersPort, DeleteUserPort {

    private final SpringDataUserRepository repository;

    UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = user.id() == null
                ? new UserJpaEntity()
                : repository.findById(user.id().value()).orElseThrow(
                        () -> new IllegalStateException("User " + user.id().value() + " does not exist to update"));
        UserMapper.copyTo(user, entity);
        return UserMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<User> byEmail(EmailAddress email) {
        return repository.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> byId(UserId id) {
        return repository.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> inOrganization(OrganizationId organizationId, UserId id) {
        return repository.findByIdAndOrganizationId(id.value(), organizationId.value()).map(UserMapper::toDomain);
    }

    @Override
    public boolean emailTaken(EmailAddress email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public boolean existsInOrganization(OrganizationId organizationId, UserId id) {
        return repository.existsByIdAndOrganizationId(id.value(), organizationId.value());
    }

    @Override
    public List<User> inOrganization(OrganizationId organizationId) {
        return repository.findByOrganizationIdOrderByNameAsc(organizationId.value()).stream()
                .map(UserMapper::toDomain).toList();
    }

    @Override
    public int countInOrganization(OrganizationId organizationId) {
        return (int) repository.countByOrganizationId(organizationId.value());
    }

    @Override
    public void delete(UserId id) {
        repository.deleteById(id.value());
    }
}
