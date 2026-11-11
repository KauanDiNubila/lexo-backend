package app.lexo.evento;

import app.lexo.IntegracaoBase;
import app.lexo.common.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Consumidor de eventos do processo-service (integracao)")
class ConsumidorEventosIntegrationTest extends IntegracaoBase {

    @Autowired
    private ConsumidorEventos consumidor;

    private DomainEvent evento(String tipo, Identidade id, String entidadeId) {
        return DomainEvent.of(tipo, id.orgId(), "CLIENTE", entidadeId, "descricao", id.userId(), id.name());
    }

    @Test
    @DisplayName("CLIENTE_EXCLUIDO apaga os processos do cliente com prazos e andamentos, so naquele escritorio")
    void clienteExcluidoApagaEmCascata() throws Exception {
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();
        String clienteExcluido = novoId();
        String outroCliente = novoId();
        String p1 = criarProcesso(a, clienteExcluido, "EV-001");
        String p2 = criarProcesso(a, clienteExcluido, "EV-002");
        String p3 = criarProcesso(a, outroCliente, "EV-003");
        String p4 = criarProcesso(b, clienteExcluido, "EV-004");
        criarPrazo(a, p1, "PRAZO", Duration.ofDays(10));
        postar(a, "/api/processos/" + p1 + "/andamentos", Map.of("title", "Juntada", "date", Instant.now().toString()))
                .andExpect(status().isCreated());

        consumidor.consumir(evento("CLIENTE_EXCLUIDO", a, clienteExcluido));

        buscar(a, "/api/processos/" + p1).andExpect(status().isNotFound());
        buscar(a, "/api/processos/" + p2).andExpect(status().isNotFound());
        buscar(a, "/api/processos/" + p3).andExpect(status().isOk());
        buscar(a, "/api/agenda?caseId=" + p1).andExpect(jsonPath("$.totalElements").value(0));
        buscar(b, "/api/processos/" + p4).andExpect(status().isOk());
    }

    @Test
    @DisplayName("outros tipos de evento sao ignorados")
    void outrosTiposSaoIgnorados() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = novoId();
        String processoId = criarProcesso(id, clienteId, "EV-005");

        consumidor.consumir(evento("CLIENTE_CRIADO", id, clienteId));
        consumidor.consumir(evento("PROCESSO_EXCLUIDO", id, processoId));

        buscar(id, "/api/processos/" + processoId).andExpect(status().isOk());
    }
}
