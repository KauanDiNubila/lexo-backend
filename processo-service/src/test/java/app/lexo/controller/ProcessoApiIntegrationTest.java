package app.lexo.controller;

import app.lexo.IntegracaoBase;
import app.lexo.common.referencia.ExisteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API de processos (integracao)")
class ProcessoApiIntegrationTest extends IntegracaoBase {

    @Test
    @DisplayName("cria um processo, lista e publica PROCESSO_CRIADO")
    void criaEListaPublicandoEvento() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = novoId();

        postar(id, "/api/processos", dadosDoProcesso(clienteId, "0001-A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.number").value("0001-A"))
                .andExpect(jsonPath("$.clientId").value(clienteId))
                .andExpect(jsonPath("$.status").value("ATIVO"));

        buscar(id, "/api/processos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(eventos).publicar(argThat(e -> "PROCESSO_CRIADO".equals(e.tipo())
                && id.orgId().equals(e.organizationId())
                && "PROCESSO".equals(e.entityType())));
    }

    @Test
    @DisplayName("cliente inexistente devolve 404 e nao grava")
    void clienteInexistente() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = novoId();
        when(clienteClient.clienteExiste(eq(clienteId), anyString())).thenReturn(new ExisteResponse(false));

        postar(id, "/api/processos", dadosDoProcesso(clienteId, "0002-A"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado"))
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));

        buscar(id, "/api/processos").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("responsavel inexistente devolve 404")
    void responsavelInexistente() throws Exception {
        Identidade id = Identidade.nova();
        String responsavelId = novoId();
        when(authClient.usuarioExiste(eq(responsavelId), anyString())).thenReturn(new ExisteResponse(false));
        Map<String, Object> dados = new HashMap<>(dadosDoProcesso(novoId(), "0003-A"));
        dados.put("responsavelId", responsavelId);

        postar(id, "/api/processos", dados)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Responsável não encontrado"));
    }

    @Test
    @DisplayName("cliente-service fora do ar devolve 503")
    void clienteServiceForaDoAr() throws Exception {
        Identidade id = Identidade.nova();
        when(clienteClient.clienteExiste(anyString(), anyString())).thenThrow(new RuntimeException("fora do ar"));

        postar(id, "/api/processos", dadosDoProcesso(novoId(), "0004-A"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("Serviço de clientes temporariamente indisponível. Tente novamente em instantes."));
    }

    @Test
    @DisplayName("sem cliente e sem numero devolve 400 com as duas mensagens")
    void validacaoDoCorpo() throws Exception {
        Identidade id = Identidade.nova();

        postar(id, "/api/processos", Map.of("clientId", "", "number", "", "status", "ATIVO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error", containsString("Selecione um cliente")))
                .andExpect(jsonPath("$.error", containsString("Número do processo é obrigatório")));
    }

    @Test
    @DisplayName("numero repetido no mesmo escritorio hoje devolve 500; em outro escritorio e permitido")
    void numeroRepetido() throws Exception {
        Identidade a = Identidade.nova();
        Identidade b = Identidade.nova();
        criarProcesso(a, novoId(), "0005-A");

        postar(a, "/api/processos", dadosDoProcesso(novoId(), "0005-A"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERRO_INTERNO"));

        postar(b, "/api/processos", dadosDoProcesso(novoId(), "0005-A")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("atualiza e busca; outro escritorio recebe 404")
    void atualizaEBusca() throws Exception {
        Identidade id = Identidade.nova();
        Identidade outro = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "0006-A");
        Map<String, Object> novos = Map.of("clientId", novoId(), "number", "0006-B", "area", "Trabalhista",
                "status", "SUSPENSO", "description", "mudou");

        colocar(id, "/api/processos/" + processoId, novos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("0006-B"))
                .andExpect(jsonPath("$.status").value("SUSPENSO"));

        buscar(id, "/api/processos/" + processoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.area").value("Trabalhista"))
                .andExpect(jsonPath("$.description").value("mudou"));

        buscar(outro, "/api/processos/" + processoId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Processo não encontrado"));

        verify(eventos).publicar(argThat(e -> "PROCESSO_ATUALIZADO".equals(e.tipo())
                && processoId.equals(e.entityId())));
    }

    @Test
    @DisplayName("excluir exige ADVOGADO; SECRETARIA recebe 403")
    void exclusaoPorPapel() throws Exception {
        Identidade admin = Identidade.nova();
        String processoId = criarProcesso(admin, novoId(), "0007-A");

        apagar(admin.comPapel("SECRETARIA"), "/api/processos/" + processoId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACESSO_NEGADO"));
        buscar(admin, "/api/processos/" + processoId).andExpect(status().isOk());

        apagar(admin.comPapel("ADVOGADO"), "/api/processos/" + processoId).andExpect(status().isNoContent());
        buscar(admin, "/api/processos/" + processoId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("excluir apaga prazos e andamentos e publica PROCESSO_EXCLUIDO")
    void exclusaoEmCascata() throws Exception {
        Identidade id = Identidade.nova();
        String processoId = criarProcesso(id, novoId(), "0008-A");
        criarPrazo(id, processoId, "PRAZO", Duration.ofDays(10));
        postar(id, "/api/processos/" + processoId + "/andamentos",
                Map.of("title", "Peticao protocolada", "date", Instant.now().toString()))
                .andExpect(status().isCreated());

        apagar(id, "/api/processos/" + processoId).andExpect(status().isNoContent());

        buscar(id, "/api/agenda?caseId=" + processoId).andExpect(jsonPath("$.totalElements").value(0));
        buscar(id, "/api/processos/" + processoId + "/andamentos").andExpect(status().isNotFound());
        verify(eventos).publicar(argThat(e -> "PROCESSO_EXCLUIDO".equals(e.tipo())
                && processoId.equals(e.entityId())));
    }

    @Test
    @DisplayName("listagem ordena do mais novo, pagina e filtra por status e por texto")
    void listagem() throws Exception {
        Identidade id = Identidade.nova();
        criarProcesso(id, novoId(), "L-001");
        postar(id, "/api/processos", Map.of("clientId", novoId(), "number", "L-002", "area", "Trabalhista",
                "status", "SUSPENSO")).andExpect(status().isCreated());
        criarProcesso(id, novoId(), "L-003");

        buscar(id, "/api/processos")
                .andExpect(jsonPath("$.content[0].number").value("L-003"))
                .andExpect(jsonPath("$.content[2].number").value("L-001"));
        buscar(id, "/api/processos?size=2&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        buscar(id, "/api/processos?status=SUSPENSO")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].number").value("L-002"));
        buscar(id, "/api/processos?q=TRABALHISTA")
                .andExpect(jsonPath("$.totalElements").value(1));
        buscar(id, "/api/processos?q=l-003")
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("opcoes e resumo refletem os processos do escritorio")
    void opcoesEResumo() throws Exception {
        Identidade id = Identidade.nova();
        String primeiro = criarProcesso(id, novoId(), "O-001");
        postar(id, "/api/processos", Map.of("clientId", novoId(), "number", "O-002", "status", "ENCERRADO"))
                .andExpect(status().isCreated());

        buscar(id, "/api/processos/opcoes")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("O-002"))
                .andExpect(jsonPath("$[1].id").value(primeiro));
        buscar(id, "/api/processos/resumo")
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.ativos").value(1))
                .andExpect(jsonPath("$.porStatus.ATIVO").value(1))
                .andExpect(jsonPath("$.porStatus.ENCERRADO").value(1))
                .andExpect(jsonPath("$.porStatus.SUSPENSO").value(0))
                .andExpect(jsonPath("$.porStatus.ARQUIVADO").value(0));
    }
}
