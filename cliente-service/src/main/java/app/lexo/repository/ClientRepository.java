package app.lexo.repository;

import app.lexo.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {

    List<Client> findByOrganizationIdOrderByNameAsc(String organizationId);

    Optional<Client> findByIdAndOrganizationId(String id, String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    long deleteByIdAndOrganizationId(String id, String organizationId);
}
