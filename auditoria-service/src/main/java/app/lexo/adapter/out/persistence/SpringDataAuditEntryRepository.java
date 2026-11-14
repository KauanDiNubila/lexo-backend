package app.lexo.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataAuditEntryRepository extends JpaRepository<AuditEntryJpaEntity, String> {

    List<AuditEntryJpaEntity> findByOrganizationId(String organizationId, Pageable pageable);
}
