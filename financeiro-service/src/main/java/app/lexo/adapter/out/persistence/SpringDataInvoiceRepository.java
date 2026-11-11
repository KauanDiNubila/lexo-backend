package app.lexo.adapter.out.persistence;

import app.lexo.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, String> {

    Page<InvoiceJpaEntity> findByOrganizationId(String organizationId, Pageable pageable);

    @Query("select coalesce(sum(i.amount), 0) from InvoiceJpaEntity i "
            + "where i.organizationId = :organizationId and i.status in :statuses")
    BigDecimal sumByStatus(@Param("organizationId") String organizationId,
                           @Param("statuses") Collection<InvoiceStatus> statuses);

    List<InvoiceJpaEntity> findByClientIdAndOrganizationIdOrderByDueDateAsc(String clientId, String organizationId);

    Optional<InvoiceJpaEntity> findByIdAndOrganizationId(String id, String organizationId);

    List<InvoiceJpaEntity> findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(
            String organizationId, Instant start, Instant end);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    long deleteByClientIdAndOrganizationId(String clientId, String organizationId);

    @Modifying
    @Query("update InvoiceJpaEntity i set i.caseId = null "
            + "where i.caseId = :caseId and i.organizationId = :organizationId")
    int detachCase(@Param("caseId") String caseId, @Param("organizationId") String organizationId);
}
