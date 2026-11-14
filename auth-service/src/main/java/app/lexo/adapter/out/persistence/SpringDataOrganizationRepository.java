package app.lexo.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrganizationRepository extends JpaRepository<OrganizationJpaEntity, String> {
}
