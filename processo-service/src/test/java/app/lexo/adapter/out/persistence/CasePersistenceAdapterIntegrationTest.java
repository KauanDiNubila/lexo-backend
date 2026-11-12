package app.lexo.adapter.out.persistence;

import app.lexo.IntegrationTestBase;
import app.lexo.application.port.in.CaseOption;
import app.lexo.application.port.out.CaseExistsPort;
import app.lexo.application.port.out.CountCasesByStatusPort;
import app.lexo.application.port.out.DeleteCasePort;
import app.lexo.application.port.out.ListCaseOptionsPort;
import app.lexo.application.port.out.ListCasesPort;
import app.lexo.application.port.out.LoadCasePort;
import app.lexo.application.port.out.LoadCasesByIdsPort;
import app.lexo.application.port.out.LoadClientCasesPort;
import app.lexo.application.port.out.SaveCasePort;
import app.lexo.common.web.PageParams;
import app.lexo.common.web.PageResult;
import app.lexo.common.web.SortOrder;
import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseFilter;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.DuplicateCaseNumberException;
import app.lexo.domain.ClientId;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Case persistence adapter (integration, Postgres)")
class CasePersistenceAdapterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SaveCasePort save;
    @Autowired
    private LoadCasePort load;
    @Autowired
    private CaseExistsPort exists;
    @Autowired
    private DeleteCasePort delete;
    @Autowired
    private ListCasesPort list;
    @Autowired
    private ListCaseOptionsPort options;
    @Autowired
    private CountCasesByStatusPort countByStatus;
    @Autowired
    private LoadClientCasesPort ofClient;
    @Autowired
    private LoadCasesByIdsPort byIds;

    private OrganizationId newOrganization() {
        return new OrganizationId(UUID.randomUUID().toString());
    }

    private Case saveCase(OrganizationId organization, ClientId client, String number, String area,
                          CaseStatus status) throws InterruptedException {
        Case saved = save.save(Case.open(organization,
                new CaseData(client, number, area, status, null, null)));
        Thread.sleep(3);
        return saved;
    }

    @Test
    @DisplayName("saves and reads back a case, keeping the responsible and filling id and creation time")
    void roundTrip() {
        OrganizationId organization = newOrganization();

        Case saved = save.save(Case.open(organization, new CaseData(new ClientId("c-1"), "0001", "Civel",
                CaseStatus.SUSPENDED, "text", new UserId("u-1"))));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.createdAt()).isNotNull();
        Case read = load.byId(organization, saved.id()).orElseThrow();
        assertThat(read.data().status()).isEqualTo(CaseStatus.SUSPENDED);
        assertThat(read.data().responsibleId()).isEqualTo(new UserId("u-1"));
        assertThat(jdbc.queryForObject("select status from cases where id = ?", String.class, saved.id().value()))
                .isEqualTo("SUSPENSO");
        assertThat(jdbc.queryForObject("select responsavel_id from cases where id = ?", String.class,
                saved.id().value())).isEqualTo("u-1");
    }

    @Test
    @DisplayName("updates through save and only reaches cases of the case's own organization")
    void updateAndScope() {
        OrganizationId owner = newOrganization();
        OrganizationId intruder = newOrganization();
        Case saved = save.save(Case.open(owner,
                new CaseData(new ClientId("c-1"), "0002", null, CaseStatus.ACTIVE, null, null)));
        saved.update(new CaseData(new ClientId("c-1"), "0002-B", null, CaseStatus.CLOSED, null, null));

        save.save(saved);

        assertThat(load.byId(owner, saved.id()).orElseThrow().data().number()).isEqualTo("0002-B");
        assertThat(load.byId(intruder, saved.id())).isEmpty();
        assertThat(exists.exists(owner, saved.id())).isTrue();
        assertThat(exists.exists(intruder, saved.id())).isFalse();
        Case asIntruder = Case.restore(saved.id(), intruder, saved.data(), saved.createdAt());
        assertThatThrownBy(() -> save.save(asIntruder)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a repeated number in the same organization is a conflict; in another organization it is allowed")
    void repeatedNumber() {
        OrganizationId organization = newOrganization();
        CaseData data = new CaseData(new ClientId("c-1"), "0009", null, CaseStatus.ACTIVE, null, null);
        save.save(Case.open(organization, data));

        assertThatThrownBy(() -> save.save(Case.open(organization, data)))
                .isInstanceOf(DuplicateCaseNumberException.class);
        assertThat(save.save(Case.open(newOrganization(), data)).id()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("deleting only reaches the organization's own case")
    void deleteScope() {
        OrganizationId owner = newOrganization();
        Case saved = save.save(Case.open(owner,
                new CaseData(new ClientId("c-1"), "0003", null, CaseStatus.ACTIVE, null, null)));

        delete.delete(newOrganization(), saved.id());
        assertThat(exists.exists(owner, saved.id())).isTrue();

        delete.delete(owner, saved.id());
        assertThat(exists.exists(owner, saved.id())).isFalse();
    }

    @Test
    @DisplayName("lists newest first, pages, and filters by status and text without crossing organizations")
    void listing() throws Exception {
        OrganizationId organization = newOrganization();
        saveCase(organization, new ClientId("c-1"), "L-001", "Civel", CaseStatus.ACTIVE);
        saveCase(organization, new ClientId("c-1"), "L-002", "Trabalhista", CaseStatus.SUSPENDED);
        saveCase(organization, new ClientId("c-1"), "L-003", "Civel", CaseStatus.ACTIVE);
        saveCase(newOrganization(), new ClientId("c-1"), "L-OTHER", "Civel", CaseStatus.ACTIVE);
        PageParams newestFirst = new PageParams(0, 20, List.of(new SortOrder("createdAt", false)));

        PageResult<Case> all = list.list(organization, new CaseFilter(null, null), newestFirst);

        assertThat(all.totalElements()).isEqualTo(3);
        assertThat(all.content()).extracting(value -> value.data().number()).containsExactly("L-003", "L-002", "L-001");
        assertThat(list.list(organization, new CaseFilter(null, null),
                new PageParams(1, 2, List.of(new SortOrder("createdAt", false)))).content()).hasSize(1);
        assertThat(list.list(organization, new CaseFilter(null, CaseStatus.SUSPENDED), newestFirst).totalElements())
                .isEqualTo(1);
        assertThat(list.list(organization, new CaseFilter("TRABALHISTA", null), newestFirst).totalElements())
                .isEqualTo(1);
        assertThat(list.list(organization, new CaseFilter("l-003", null), newestFirst).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("options come newest first; counts cover every status in order; client and id lookups are scoped")
    void optionsCountsAndLookups() throws Exception {
        OrganizationId organization = newOrganization();
        ClientId client = new ClientId("c-9");
        Case first = saveCase(organization, client, "O-001", null, CaseStatus.ACTIVE);
        Case second = saveCase(organization, client, "O-002", null, CaseStatus.CLOSED);
        saveCase(organization, new ClientId("c-other"), "O-003", null, CaseStatus.ACTIVE);

        assertThat(options.options(organization)).extracting(CaseOption::number)
                .containsExactly("O-003", "O-002", "O-001");
        Map<CaseStatus, Long> counts = countByStatus.countByStatus(organization);
        assertThat(counts.keySet()).containsExactly(CaseStatus.values());
        assertThat(counts.get(CaseStatus.ACTIVE)).isEqualTo(2);
        assertThat(counts.get(CaseStatus.CLOSED)).isEqualTo(1);
        assertThat(counts.get(CaseStatus.SUSPENDED)).isZero();
        assertThat(ofClient.ofClient(organization, client)).extracting(value -> value.data().number())
                .containsExactly("O-002", "O-001");
        assertThat(ofClient.ofClient(newOrganization(), client)).isEmpty();
        assertThat(byIds.byIds(List.of(first.id(), second.id()))).hasSize(2);
    }
}
