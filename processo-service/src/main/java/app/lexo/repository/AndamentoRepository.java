package app.lexo.repository;

import app.lexo.domain.LegacyAndamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AndamentoRepository extends JpaRepository<LegacyAndamento, String> {

    List<LegacyAndamento> findByCaseIdAndOrganizationIdOrderByDateDesc(String caseId, String organizationId);

    List<LegacyAndamento> findByCaseIdInAndOrganizationIdOrderByDateDesc(List<String> caseIds, String organizationId);

    Optional<LegacyAndamento> findByIdAndOrganizationId(String id, String organizationId);

    long deleteByCaseIdAndOrganizationId(String caseId, String organizationId);
}
