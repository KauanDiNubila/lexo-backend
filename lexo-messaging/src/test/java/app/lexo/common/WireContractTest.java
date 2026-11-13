package app.lexo.common;

import app.lexo.common.event.DomainEvent;
import app.lexo.common.event.EventType;
import app.lexo.common.portal.PortalDtos.PortalCase;
import app.lexo.common.reference.ExistsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wire contract shared between services")
class WireContractTest {

    private final ObjectMapper json = JsonMapper.builder().findAndAddModules().build();

    @Test
    @DisplayName("an event published by an old service is still read, with the Portuguese field names")
    void readsLegacyEvent() throws Exception {
        String legacy = """
                {"tipo":"CLIENTE_EXCLUIDO","organizationId":"org-1","entityType":"CLIENTE","entityId":"c-1",
                 "descricao":"removed","userId":"u-1","userName":"Ana","ocorridoEm":"2026-01-02T03:04:05Z"}
                """;

        DomainEvent event = json.readValue(legacy, DomainEvent.class);

        assertThat(event.type()).isEqualTo("CLIENTE_EXCLUIDO");
        assertThat(event.description()).isEqualTo("removed");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(event.is(EventType.CLIENT_DELETED)).isTrue();
        assertThat(event.is(EventType.CASE_DELETED)).isFalse();
    }

    @Test
    @DisplayName("an event is written with the same field names the other services expect")
    void writesLegacyFieldNames() throws Exception {
        DomainEvent event = DomainEvent.of(EventType.CASE_CREATED.code(), "org-1", "PROCESSO", "p-1", "created",
                "u-1", "Ana");

        JsonNode node = json.readTree(json.writeValueAsString(event));

        assertThat(node.fieldNames()).toIterable().containsExactlyInAnyOrder("tipo", "organizationId",
                "entityType", "entityId", "descricao", "userId", "userName", "ocorridoEm");
        assertThat(node.get("tipo").asText()).isEqualTo("PROCESSO_CRIADO");
    }

    @Test
    @DisplayName("event type codes are the values stored and exchanged today")
    void eventTypeCodes() {
        assertThat(EventType.values()).extracting(EventType::code).containsExactly(
                "CLIENTE_CRIADO", "CLIENTE_ATUALIZADO", "CLIENTE_EXCLUIDO", "PROCESSO_CRIADO", "PROCESSO_ATUALIZADO", "PROCESSO_EXCLUIDO",
                "CONVIDOU_USUARIO", "REVOGOU_CONVITE", "ALTEROU_PAPEL", "REMOVEU_USUARIO", "ATIVOU_2FA",
                "DESATIVOU_2FA");
    }

    @Test
    @DisplayName("the existence answer keeps the field existe")
    void existsResponse() throws Exception {
        assertThat(json.writeValueAsString(new ExistsResponse(true))).isEqualTo("{\"existe\":true}");
        assertThat(json.readValue("{\"existe\":false}", ExistsResponse.class).exists()).isFalse();
    }

    @Test
    @DisplayName("the portal case keeps the fields prazos and andamentos")
    void portalCase() throws Exception {
        PortalCase portalCase = new PortalCase("1", "civil", "ATIVO", Instant.parse("2026-01-01T00:00:00Z"),
                List.of(), List.of());

        JsonNode node = json.readTree(json.writeValueAsString(portalCase));

        assertThat(node.has("prazos")).isTrue();
        assertThat(node.has("andamentos")).isTrue();
        assertThat(node.has("deadlines")).isFalse();
    }
}
