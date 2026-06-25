package app.lexo.repository;

import app.lexo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByOrganizationIdOrderByNameAsc(String organizationId);

    Optional<User> findByIdAndOrganizationId(String id, String organizationId);
}
