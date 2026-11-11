package app.lexo.repository;

import app.lexo.domain.LegacyInvoice;
import app.lexo.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<LegacyInvoice, String> {

    List<LegacyInvoice> findByOrganizationIdOrderByDueDateAsc(String organizationId);

    Page<LegacyInvoice> findByOrganizationId(String organizationId, Pageable pageable);

    @Query("select coalesce(sum(i.amount), 0) from LegacyInvoice i "
            + "where i.organizationId = :organizationId and i.status in :status")
    java.math.BigDecimal somarPorStatus(
            @Param("organizationId") String organizationId,
            @Param("status") java.util.Collection<InvoiceStatus> status);

    List<LegacyInvoice> findByClientIdAndOrganizationIdOrderByDueDateAsc(String clientId, String organizationId);

    Optional<LegacyInvoice> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    List<LegacyInvoice> findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(
            String organizationId, Instant start, Instant end);

    long deleteByClientIdAndOrganizationId(String clientId, String organizationId);

    @Modifying
    @Query("update LegacyInvoice i set i.caseId = null "
            + "where i.caseId = :caseId and i.organizationId = :organizationId")
    int desvincularProcesso(@Param("caseId") String caseId,
                            @Param("organizationId") String organizationId);
}
