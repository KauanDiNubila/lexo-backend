package app.lexo.repository;

import app.lexo.domain.Andamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AndamentoRepository extends JpaRepository<Andamento, String> {

    List<Andamento> findByCaseIdAndOrganizationIdOrderByDateDesc(String caseId, String organizationId);

    List<Andamento> findByCaseIdInAndOrganizationIdOrderByDateDesc(List<String> caseIds, String organizationId);

    Optional<Andamento> findByIdAndOrganizationId(String id, String organizationId);
}
