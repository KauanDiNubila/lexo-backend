package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface SpringDataMovementRepository extends JpaRepository<MovementJpaEntity, String> {

    List<MovementJpaEntity> findByCaseIdAndOrganizationIdOrderByDateDesc(String caseId, String organizationId);

    List<MovementJpaEntity> findByCaseIdInAndOrganizationIdOrderByDateDesc(Collection<String> caseIds,
                                                                           String organizationId);

    Optional<MovementJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    long deleteByCaseIdAndOrganizationId(String caseId, String organizationId);
}
