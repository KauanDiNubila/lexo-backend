package app.lexo.repository;

import app.lexo.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByOrganizationIdOrderByDueDateAsc(String organizationId);

    Optional<Invoice> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);

    /** Para o relatorio financeiro por periodo. */
    List<Invoice> findByOrganizationIdAndDueDateBetweenOrderByDueDateAsc(
            String organizationId, Instant start, Instant end);
}
