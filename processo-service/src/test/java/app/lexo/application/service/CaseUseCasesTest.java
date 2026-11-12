package app.lexo.application.service;

import app.lexo.application.port.in.Actor;
import app.lexo.application.port.in.CaseResponse;
import app.lexo.application.port.in.CreateCaseCommand;
import app.lexo.application.port.in.DeleteCaseCommand;
import app.lexo.application.port.in.DeleteClientCasesCommand;
import app.lexo.application.port.in.UpdateCaseCommand;
import app.lexo.application.port.out.CaseEvent;
import app.lexo.domain.Case;
import app.lexo.domain.CaseData;
import app.lexo.domain.CaseId;
import app.lexo.domain.CaseMovement;
import app.lexo.domain.CaseNotFoundException;
import app.lexo.domain.CaseStatus;
import app.lexo.domain.ClientId;
import app.lexo.domain.ClientNotFoundException;
import app.lexo.domain.Deadline;
import app.lexo.domain.DeadlineData;
import app.lexo.domain.DeadlineType;
import app.lexo.domain.MovementData;
import app.lexo.domain.OrganizationId;
import app.lexo.domain.ResponsibleNotFoundException;
import app.lexo.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Case use cases (in-memory ports)")
class CaseUseCasesTest {

    private static final OrganizationId ORGANIZATION = new OrganizationId("org-1");
    private static final OrganizationId OTHER_ORGANIZATION = new OrganizationId("org-2");
    private static final ClientId CLIENT = new ClientId("client-1");
    private static final Actor ACTOR = new Actor("user-1", "Ana");

    private final InMemoryCases cases = new InMemoryCases();
    private final InMemoryDeadlines deadlines = new InMemoryDeadlines();
    private final InMemoryMovements movements = new InMemoryMovements();
    private final List<CaseEvent> events = new ArrayList<>();
    private final Set<String> knownClients = new HashSet<>(Set.of("client-1"));
    private final Set<String> knownUsers = new HashSet<>(Set.of("user-1"));
    private final CaseReferences references = new CaseReferences(
            (organization, client) -> knownClients.contains(client.value()),
            (organization, user) -> knownUsers.contains(user.value()));
    private final CaseRemoval removal = new CaseRemoval(deadlines, movements, cases);
    private final CreateCaseService create = new CreateCaseService(references, cases, events::add);
    private final UpdateCaseService update = new UpdateCaseService(references, cases, cases, events::add);
    private final DeleteCaseService delete = new DeleteCaseService(cases, removal, events::add);
    private final DeleteClientCasesService deleteOfClient = new DeleteClientCasesService(cases, removal);

    private CaseData data(ClientId client, String number, UserId responsible) {
        return new CaseData(client, number, "Civel", CaseStatus.ACTIVE, null, responsible);
    }

    private CaseResponse open(OrganizationId organization, ClientId client, String number) {
        return create.create(new CreateCaseCommand(organization, ACTOR, data(client, number, null)));
    }

    private void addChildren(OrganizationId organization, String caseId) {
        deadlines.save(Deadline.schedule(organization, new DeadlineData(new CaseId(caseId), "t",
                DeadlineType.DEADLINE, Instant.parse("2027-01-01T00:00:00Z"), null)));
        movements.save(CaseMovement.record(organization, new CaseId(caseId),
                new MovementData("m", null, Instant.parse("2027-01-01T00:00:00Z"))));
    }

    @Test
    @DisplayName("creating saves the case and publishes the created event")
    void creates() {
        CaseResponse response = create.create(new CreateCaseCommand(ORGANIZATION, ACTOR,
                data(CLIENT, "0001", new UserId("user-1"))));

        assertThat(response.number()).isEqualTo("0001");
        assertThat(response.responsibleId()).isEqualTo("user-1");
        assertThat(cases.all()).hasSize(1);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("PROCESSO_CRIADO");
            assertThat(event.description()).isEqualTo("Processo criado: 0001");
            assertThat(event.actor()).isEqualTo(ACTOR);
        });
    }

    @Test
    @DisplayName("an unknown client or responsible is refused before anything is saved")
    void refusesUnknownReferences() {
        assertThatThrownBy(() -> open(ORGANIZATION, new ClientId("ghost"), "0002"))
                .isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> create.create(new CreateCaseCommand(ORGANIZATION, ACTOR,
                data(CLIENT, "0003", new UserId("ghost")))))
                .isInstanceOf(ResponsibleNotFoundException.class);

        assertThat(cases.all()).isEmpty();
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("updating replaces the data and publishes the updated event")
    void updates() {
        String id = open(ORGANIZATION, CLIENT, "0004").id();

        CaseResponse response = update.update(new UpdateCaseCommand(ORGANIZATION, new CaseId(id), ACTOR,
                new CaseData(CLIENT, "0004-B", "Trabalhista", CaseStatus.SUSPENDED, "text", null)));

        assertThat(response.number()).isEqualTo("0004-B");
        assertThat(response.status()).isEqualTo(CaseStatus.SUSPENDED);
        assertThat(events).last().extracting(CaseEvent::type).isEqualTo("PROCESSO_ATUALIZADO");
    }

    @Test
    @DisplayName("updating checks the references before looking for the case")
    void updateChecksReferencesFirst() {
        assertThatThrownBy(() -> update.update(new UpdateCaseCommand(ORGANIZATION, new CaseId("ghost-case"), ACTOR,
                data(new ClientId("ghost"), "x", null)))).isInstanceOf(ClientNotFoundException.class);
        assertThatThrownBy(() -> update.update(new UpdateCaseCommand(ORGANIZATION, new CaseId("ghost-case"), ACTOR,
                data(CLIENT, "x", null)))).isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    @DisplayName("a case of another organization cannot be updated or deleted")
    void organizationScope() {
        String id = open(ORGANIZATION, CLIENT, "0005").id();

        assertThatThrownBy(() -> update.update(new UpdateCaseCommand(OTHER_ORGANIZATION, new CaseId(id), ACTOR,
                data(CLIENT, "hack", null)))).isInstanceOf(CaseNotFoundException.class);
        assertThatThrownBy(() -> delete.delete(new DeleteCaseCommand(OTHER_ORGANIZATION, new CaseId(id), ACTOR)))
                .isInstanceOf(CaseNotFoundException.class);

        assertThat(cases.all()).hasSize(1);
    }

    @Test
    @DisplayName("deleting removes deadlines, movements and the case, and publishes the deleted event")
    void deletesWithChildren() {
        String id = open(ORGANIZATION, CLIENT, "0006").id();
        addChildren(ORGANIZATION, id);
        events.clear();

        delete.delete(new DeleteCaseCommand(ORGANIZATION, new CaseId(id), ACTOR));

        assertThat(cases.all()).isEmpty();
        assertThat(deadlines.all()).isEmpty();
        assertThat(movements.all()).isEmpty();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("PROCESSO_EXCLUIDO");
            assertThat(event.description()).isEqualTo("Processo exclu\u00eddo: 0006");
        });
    }

    @Test
    @DisplayName("deleting the cases of a client removes only that client's cases in that organization, with no events")
    void deletesCasesOfClient() {
        String first = open(ORGANIZATION, CLIENT, "0007").id();
        open(ORGANIZATION, CLIENT, "0008");
        knownClients.add("client-2");
        String other = open(ORGANIZATION, new ClientId("client-2"), "0009").id();
        String foreign = open(OTHER_ORGANIZATION, CLIENT, "0010").id();
        addChildren(ORGANIZATION, first);
        events.clear();

        int removed = deleteOfClient.deleteOfClient(new DeleteClientCasesCommand(ORGANIZATION, CLIENT));

        assertThat(removed).isEqualTo(2);
        assertThat(cases.all()).extracting(value -> value.id().value()).containsExactlyInAnyOrder(other, foreign);
        assertThat(deadlines.all()).isEmpty();
        assertThat(movements.all()).isEmpty();
        assertThat(events).isEmpty();
    }
}
