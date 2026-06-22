package app.lexo.repository;

import app.lexo.domain.Case;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, String> {

    List<Case> findByOrganizationIdOrderByCreatedAtDesc(String organizationId);

    Optional<Case> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
