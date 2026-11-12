package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserJpaEntity> findByOrganizationIdOrderByNameAsc(String organizationId);

    Optional<UserJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long countByOrganizationId(String organizationId);
}
