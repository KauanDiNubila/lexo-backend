package app.lexo.repository;

import app.lexo.domain.Invoice;
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

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByOrganizationIdOrderByDueDateAsc(String organizationId);

    Page<Invoice> findByOrganizationId(String organizationId, Pageable pageable);

    @Query("select coalesce(sum(i.amount), 0) from Invoice i "
            + "where i.organizationId = :organizationId and i.status in :status")
    java.math.BigDecimal somarPorStatus(
            @Param("organizationId") String organizationId,
            @Param("status") java.util.Collection<InvoiceStatus> status);

    List<Invoice> findByClientIdAndOrganizationIdOrderByDueDateAsc(String clientId, String organizationId);

    Optional<Invoice> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    List<Invoice> findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(
            String organizationId, Instant start, Instant end);

    long deleteByClientIdAndOrganizationId(String clientId, String organizationId);

    @Modifying
    @Query("update Invoice i set i.caseId = null "
            + "where i.caseId = :caseId and i.organizationId = :organizationId")
    int desvincularProcesso(@Param("caseId") String caseId,
                            @Param("organizationId") String organizationId);
}
