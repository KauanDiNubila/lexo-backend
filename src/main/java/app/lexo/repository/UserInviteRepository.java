package app.lexo.repository;

import app.lexo.domain.UserInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserInviteRepository extends JpaRepository<UserInvite, String> {

    Optional<UserInvite> findByToken(String token);

    Optional<UserInvite> findByIdAndOrganizationId(String id, String organizationId);

    List<UserInvite> findByOrganizationIdAndAcceptedAtIsNullOrderByCreatedAtDesc(String organizationId);

    Optional<UserInvite> findFirstByEmailAndOrganizationIdAndAcceptedAtIsNullAndExpiresAtAfter(
            String email, String organizationId, Instant now);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
