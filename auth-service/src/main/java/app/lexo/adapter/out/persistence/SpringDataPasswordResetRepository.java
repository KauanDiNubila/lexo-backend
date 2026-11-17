package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataPasswordResetRepository extends JpaRepository<PasswordResetJpaEntity, String> {

    Optional<PasswordResetJpaEntity> findByTokenHash(String tokenHash);

    void deleteByUserId(String userId);
}
