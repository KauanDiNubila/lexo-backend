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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final Instant OCCURRED_AT = Instant.parse("2026-11-12T10:15:30Z");

    @Test
    @DisplayName("turns the event into a command, using the event type as the action and the event date")
    void mapsTheEvent() {
        listener.onEvent(new DomainEvent("CLIENTE_CRIADO", "escritorio-1", "CLIENTE", "c-1", "Cliente criado: Maria",
                "u-1", "Ana", OCCURRED_AT));

        assertThat(registered).containsExactly(new RegisterAuditEntryCommand(new OrganizationId("escritorio-1"),
                "u-1", "Ana", "CLIENTE_CRIADO", "CLIENTE", "c-1", "Cliente criado: Maria", OCCURRED_AT));
    }

    @Test
    @DisplayName("lets a failure to register reach the error handler")
    void propagatesFailures() {
        failure = new IllegalStateException("banco fora do ar");

        assertThatThrownBy(() -> listener.onEvent(new DomainEvent("CLIENTE_CRIADO", "escritorio-1", "CLIENTE", "c-1",
                "d", "u-1", "Ana", Instant.now()))).isSameAs(failure);
    }

    @Test
    @DisplayName("lets an event without organization reach the error handler, and registers nothing")
    void propagatesAnEventWithoutOrganization() {
        assertThatThrownBy(() -> listener.onEvent(new DomainEvent("CLIENTE_CRIADO", null, "CLIENTE", "c-1", "d", "u-1",
                "Ana", Instant.now()))).isInstanceOf(IllegalArgumentException.class);

        assertThat(registered).isEmpty();
    }
}
