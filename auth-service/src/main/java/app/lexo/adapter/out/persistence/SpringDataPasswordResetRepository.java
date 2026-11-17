package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

interface SpringDataPasswordResetRepository extends JpaRepository<PasswordResetJpaEntity, String> {

    Optional<PasswordResetJpaEntity> findByTokenHash(String tokenHash);

    void deleteByUserId(String userId);

    void deleteByUserIdAndExpiresAtBefore(String userId, Instant now);
}
