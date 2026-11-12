package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.out.DeleteMovementPort;
import app.lexo.application.port.out.DeleteMovementsOfCasePort;
import app.lexo.application.port.out.ListMovementsPort;
import app.lexo.application.port.out.LoadMovementsOfCasesPort;
import app.lexo.application.port.out.SaveMovementPort;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.MovementData;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Movement persistence adapter (integration, Postgres)")
class MovementPersistenceAdapterIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Autowired
    private SaveMovementPort save;
    @Autowired
    private DeleteMovementPort delete;
    @Autowired
    private ListMovementsPort list;
    @Autowired
    private DeleteMovementsOfCasePort deleteOfCase;
    @Autowired
    private LoadMovementsOfCasesPort ofCases;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private CaseMovement saveMovement(OrganizationId organization, CaseId caseId, String title, Duration fromNow) {
        return save.save(CaseMovement.record(organization, caseId,
                new MovementData(title, "text", NOW.plus(fromNow))));
    }

    @Test
    @DisplayName("saves a movement and lists the case's movements from the most recent to the oldest")
    void saveAndList() {
        OrganizationId organization = newOrganization();
        CaseId caseId = new CaseId("case-1");
        CaseMovement saved = saveMovement(organization, caseId, "Distribution", Duration.ofDays(-10));
        saveMovement(organization, caseId, "Sentence", Duration.ZERO);
        saveMovement(organization, new CaseId("case-2"), "Other case", Duration.ZERO);
        saveMovement(newOrganization(), caseId, "Other organization", Duration.ZERO);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(list.ofCase(organization, caseId)).extracting(movement -> movement.data().title())
                .containsExactly("Sentence", "Distribution");
    }

    @Test
    @Transactional
    @DisplayName("deleting by id answers whether something was deleted and only reaches the organization's own")
    void deleteScope() {
        OrganizationId owner = newOrganization();
        CaseMovement saved = saveMovement(owner, new CaseId("case-3"), "Order", Duration.ZERO);

        CaseId caseId = new CaseId("case-3");

        assertThat(delete.delete(newOrganization(), caseId, saved.id())).isFalse();
        assertThat(delete.delete(owner, new CaseId("another-case"), saved.id())).isFalse();
        assertThat(delete.delete(owner, caseId, saved.id())).isTrue();
        assertThat(delete.delete(owner, caseId, saved.id())).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("deleting by case only reaches the organization's own movements")
    void deleteOfCaseScope() {
        OrganizationId owner = newOrganization();
        CaseId caseId = new CaseId("case-4");
        saveMovement(owner, caseId, "Order", Duration.ZERO);

        deleteOfCase.deleteOfCase(newOrganization(), caseId);
        assertThat(list.ofCase(owner, caseId)).hasSize(1);

        deleteOfCase.deleteOfCase(owner, caseId);
        assertThat(list.ofCase(owner, caseId)).isEmpty();
    }

    @Test
    @DisplayName("loads the movements of several cases of one organization")
    void ofSeveralCases() {
        OrganizationId organization = newOrganization();
        CaseId first = new CaseId("case-5");
        CaseId second = new CaseId("case-6");
        saveMovement(organization, first, "A", Duration.ZERO);
        saveMovement(organization, second, "B", Duration.ZERO);
        saveMovement(newOrganization(), first, "C", Duration.ZERO);

        assertThat(ofCases.ofCases(organization, List.of(first, second))).hasSize(2);
    }
}
