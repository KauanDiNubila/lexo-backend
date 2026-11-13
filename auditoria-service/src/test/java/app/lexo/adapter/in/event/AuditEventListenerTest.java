package app.lexo.adapter.in.event;

import app.lexo.application.port.in.RegisterAuditEntryCommand;
import app.lexo.common.event.DomainEvent;
import app.lexo.domain.OrganizationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AuditEventListener")
class AuditEventListenerTest {

    private final List<RegisterAuditEntryCommand> registered = new ArrayList<>();
    private RuntimeException failure;

    private final AuditEventListener listener = new AuditEventListener(command -> {
        if (failure != null) {
            throw failure;
        }
        registered.add(command);
    });

    @Test
    @DisplayName("turns the event into a command, using the event type as the action")
    void mapsTheEvent() {
        listener.onEvent(new DomainEvent("CLIENTE_CRIADO", "escritorio-1", "CLIENTE", "c-1", "Cliente criado: Maria",
                "u-1", "Ana", Instant.now()));

        assertThat(registered).containsExactly(new RegisterAuditEntryCommand(new OrganizationId("escritorio-1"),
                "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1", "Cliente criado: Maria"));
    }

    @Test
    @DisplayName("does not fail the consumer when the entry cannot be registered")
    void swallowsFailures() {
        failure = new IllegalStateException("banco fora do ar");

        assertThatCode(() -> listener.onEvent(new DomainEvent("CLIENTE_CRIADO", "escritorio-1", "CLIENTE", "c-1",
                "d", "u-1", "Ana", Instant.now()))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("does not fail the consumer when the event has no organization, and registers nothing")
    void swallowsAnEventWithoutOrganization() {
        assertThatCode(() -> listener.onEvent(new DomainEvent("CLIENTE_CRIADO", null, "CLIENTE", "c-1", "d", "u-1",
                "Ana", Instant.now()))).doesNotThrowAnyException();

        assertThat(registered).isEmpty();
    }
}
