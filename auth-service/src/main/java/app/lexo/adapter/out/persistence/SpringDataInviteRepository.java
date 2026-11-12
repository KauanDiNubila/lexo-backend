package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface SpringDataInviteRepository extends JpaRepository<InviteJpaEntity, String> {

    Optional<InviteJpaEntity> findByToken(String token);

    Optional<InviteJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    List<InviteJpaEntity> findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(String organizationId);

    boolean existsByEmailAndOrganizationIdAndAcceptedAtIsNullAndExpiresAtAfter(String email,
                                                                               String organizationId,
                                                                               Instant now);

    void deleteByIdAndOrganizationId(String id, String organizationId);
}
