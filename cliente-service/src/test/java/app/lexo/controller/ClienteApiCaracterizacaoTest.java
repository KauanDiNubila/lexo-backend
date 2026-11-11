package app.lexo.controller;

import app.lexo.IntegracaoBase;
import app.lexo.common.portal.PortalDtos.HonorarioPortal;
import app.lexo.common.portal.PortalDtos.ProcessoPortal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API de clientes: comportamento fixado antes da migracao hexagonal")
class ClienteApiCaracterizacaoTest extends IntegracaoBase {

    private Map<String, Object> dados(String nome, String documento, String email, String telefone) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("name", nome);
        dados.put("document", documento);
        dados.put("email", email);
        dados.put("phone", telefone);
        return dados;
    }

    @Test
    @DisplayName("criar devolve o cliente completo e publica CLIENTE_CRIADO")
    void criarPublicaEvento() throws Exception {
        Identidade id = Identidade.nova();

        String clienteId = idDe(postar(id, "/api/clientes", dados("Maria Souza", "529.982.247-25", "maria@escritorio.com", "11999990000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.document").value("529.982.247-25"))
                .andExpect(jsonPath("$.email").value("maria@escritorio.com"))
                .andExpect(jsonPath("$.phone").value("11999990000"))
                .andExpect(jsonPath("$.portalAtivo").value(false))
                .andExpect(jsonPath("$.portalExpiraEm").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").exists()));

        verify(eventos).publicar(argThat(e -> "CLIENTE_CRIADO".equals(e.tipo())
                && id.orgId().equals(e.organizationId())
                && "CLIENTE".equals(e.entityType())
                && clienteId.equals(e.entityId())
                && "Cliente criado: Maria Souza".equals(e.descricao())
                && id.userId().equals(e.userId())
                && "Ana Advogada".equals(e.userName())));
    }

    @Test
    @DisplayName("campos opcionais em branco viram nulos")
    void camposEmBrancoViramNulos() throws Exception {
        Identidade id = Identidade.nova();

        postar(id, "/api/clientes", dados("Joao Lima", "", "  ", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.phone").value(nullValue()));
    }

    @Test
    @DisplayName("nome curto devolve 400 com a mensagem de validacao")
    void nomeCurto() throws Exception {
        Identidade id = Identidade.nova();

        postar(id, "/api/clientes", Map.of("name", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACAO"))
                .andExpect(jsonPath("$.error", containsString("Nome muito curto")));
    }

    @Test
    @DisplayName("atualizar altera os campos, nao publica evento e outro escritorio recebe 404")
    void atualizar() throws Exception {
        Identidade id = Identidade.nova();
        Identidade outro = Identidade.nova();
        String clienteId = criarCliente(id, "Maria Souza");

        colocar(id, "/api/clientes/" + clienteId, dados("Maria Souza Lima", "52998224725", "novo@escritorio.com", "11888880000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteId))
                .andExpect(jsonPath("$.name").value("Maria Souza Lima"))
                .andExpect(jsonPath("$.document").value("52998224725"));

        buscar(id, "/api/clientes/" + clienteId)
                .andExpect(jsonPath("$.email").value("novo@escritorio.com"))
                .andExpect(jsonPath("$.phone").value("11888880000"));
        verify(eventos, times(1)).publicar(any());

        colocar(outro, "/api/clientes/" + clienteId, dados("Invasor", null, null, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente não encontrado"))
                .andExpect(jsonPath("$.code").value("NAO_ENCONTRADO"));
        buscar(outro, "/api/clientes/" + clienteId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("atualizar com CPF invalido devolve 400 antes de procurar o cliente")
    void atualizarComDocumentoInvalido() throws Exception {
        Identidade id = Identidade.nova();

        colocar(id, "/api/clientes/" + novoId(), dados("Fulano Tal", "11111111111", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CPF ou CNPJ inválido"));
    }

    @Test
    @DisplayName("excluir publica CLIENTE_EXCLUIDO e uma segunda exclusao devolve 404")
    void excluir() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Carlos Dias");

        apagar(id, "/api/clientes/" + clienteId).andExpect(status().isNoContent());

        verify(eventos).publicar(argThat(e -> "CLIENTE_EXCLUIDO".equals(e.tipo())
                && clienteId.equals(e.entityId())
                && "Cliente excluído: Carlos Dias".equals(e.descricao())));
        apagar(id, "/api/clientes/" + clienteId).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("outro escritorio nao exclui o cliente")
    void excluirDeOutroEscritorio() throws Exception {
        Identidade dono = Identidade.nova();
        Identidade outro = Identidade.nova();
        String clienteId = criarCliente(dono, "Carlos Dias");

        apagar(outro, "/api/clientes/" + clienteId).andExpect(status().isNotFound());

        buscar(dono, "/api/clientes/" + clienteId).andExpect(status().isOk());
    }

    @Test
    @DisplayName("lista por nome, pagina, ordena e busca em nome, documento, e-mail e telefone")
    void listagem() throws Exception {
        Identidade id = Identidade.nova();
        postar(id, "/api/clientes", dados("Carla Mendes", "529.982.247-25", "carla@x.com", "11911110000")).andExpect(status().isCreated());
        postar(id, "/api/clientes", dados("Ana Paula", "", "ANA@LOJA.COM", "21922220000")).andExpect(status().isCreated());
        postar(id, "/api/clientes", dados("Bruno Reis", "", "", "31933330000")).andExpect(status().isCreated());

        buscar(id, "/api/clientes")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Ana Paula"))
                .andExpect(jsonPath("$.content[2].name").value("Carla Mendes"));
        buscar(id, "/api/clientes?sort=name,desc")
                .andExpect(jsonPath("$.content[0].name").value("Carla Mendes"));
        buscar(id, "/api/clientes?size=2&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        buscar(id, "/api/clientes?q=BRUNO").andExpect(jsonPath("$.totalElements").value(1));
        buscar(id, "/api/clientes?q=529.982").andExpect(jsonPath("$.content[0].name").value("Carla Mendes"));
        buscar(id, "/api/clientes?q=ana@loja").andExpect(jsonPath("$.content[0].name").value("Ana Paula"));
        buscar(id, "/api/clientes?q=9333").andExpect(jsonPath("$.content[0].name").value("Bruno Reis"));
        buscar(id, "/api/clientes?q=  ").andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("opcoes lista id e nome por ordem alfabetica e resumo conta os clientes")
    void opcoesEResumo() throws Exception {
        Identidade id = Identidade.nova();
        String zeca = criarCliente(id, "Zeca Alves");
        String ana = criarCliente(id, "Ana Paula");

        buscar(id, "/api/clientes/opcoes")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(ana))
                .andExpect(jsonPath("$[0].name").value("Ana Paula"))
                .andExpect(jsonPath("$[1].id").value(zeca));
        buscar(id, "/api/clientes/resumo").andExpect(jsonPath("$.total").value(2));
        buscar(Identidade.nova(), "/api/clientes/resumo").andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("o portal gera o link, agrega processos e honorarios, e um novo link invalida o anterior")
    void portal() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");
        when(processoClient.processosDoCliente(eq(clienteId), eq(id.orgId()))).thenReturn(List.of(
                new ProcessoPortal("P-1", "Civel", "ATIVO", Instant.now(), List.of(), List.of())));
        when(financeiroClient.honorariosDoCliente(eq(clienteId), eq(id.orgId()))).thenReturn(List.of(
                new HonorarioPortal("Consulta", new BigDecimal("100.00"), "PENDENTE", Instant.now()),
                new HonorarioPortal("Audiencia", new BigDecimal("50.50"), "ATRASADO", Instant.now()),
                new HonorarioPortal("Peticao", new BigDecimal("200.00"), "PAGO", Instant.now())));

        String primeiro = corpoDe(postar(id, "/api/clientes/" + clienteId + "/portal", Map.of()).andExpect(status().isOk()))
                .get("token").asText();
        assertThat(primeiro).hasSize(64);
        buscar(id, "/api/clientes/" + clienteId)
                .andExpect(jsonPath("$.portalAtivo").value(true))
                .andExpect(jsonPath("$.portalExpiraEm").exists());

        mvc.perform(get("/api/portal/{token}", primeiro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Helena Prado"))
                .andExpect(jsonPath("$.processos.length()").value(1))
                .andExpect(jsonPath("$.processos[0].number").value("P-1"))
                .andExpect(jsonPath("$.honorarios.length()").value(3))
                .andExpect(jsonPath("$.resumo.totalProcessos").value(1))
                .andExpect(jsonPath("$.resumo.emAberto").value(150.5));

        String segundo = corpoDe(postar(id, "/api/clientes/" + clienteId + "/portal", Map.of())).get("token").asText();
        assertThat(segundo).isNotEqualTo(primeiro);
        mvc.perform(get("/api/portal/{token}", primeiro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));
        mvc.perform(get("/api/portal/{token}", segundo)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("revogar apaga o link e o cliente deixa de ter portal ativo")
    void revogarPortal() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");
        String token = corpoDe(postar(id, "/api/clientes/" + clienteId + "/portal", Map.of())).get("token").asText();

        apagar(id, "/api/clientes/" + clienteId + "/portal").andExpect(status().isNoContent());

        mvc.perform(get("/api/portal/{token}", token)).andExpect(status().isNotFound());
        buscar(id, "/api/clientes/" + clienteId)
                .andExpect(jsonPath("$.portalAtivo").value(false))
                .andExpect(jsonPath("$.portalExpiraEm").value(nullValue()));
    }

    @Test
    @DisplayName("link expirado devolve 404")
    void portalExpirado() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");
        String token = corpoDe(postar(id, "/api/clientes/" + clienteId + "/portal", Map.of())).get("token").asText();
        jdbc.update("update clients set portal_token_expires_at = now() - interval '1 day' where id = ?", clienteId);

        mvc.perform(get("/api/portal/{token}", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link inválido ou expirado"));
        buscar(id, "/api/clientes/" + clienteId).andExpect(jsonPath("$.portalAtivo").value(false));
    }

    @Test
    @DisplayName("gerar ou revogar o portal de outro escritorio devolve 404")
    void portalDeOutroEscritorio() throws Exception {
        Identidade dono = Identidade.nova();
        Identidade outro = Identidade.nova();
        String clienteId = criarCliente(dono, "Helena Prado");

        postar(outro, "/api/clientes/" + clienteId + "/portal", Map.of()).andExpect(status().isNotFound());
        apagar(outro, "/api/clientes/" + clienteId + "/portal").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("se um servico do portal falha, a secao dele vem vazia e o resto continua")
    void portalComServicoForaDoAr() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");
        String token = corpoDe(postar(id, "/api/clientes/" + clienteId + "/portal", Map.of())).get("token").asText();
        when(processoClient.processosDoCliente(anyString(), anyString())).thenThrow(new RuntimeException("fora do ar"));
        when(financeiroClient.honorariosDoCliente(anyString(), anyString())).thenReturn(List.of(
                new HonorarioPortal("Consulta", new BigDecimal("80.00"), "PENDENTE", Instant.now())));

        mvc.perform(get("/api/portal/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processos.length()").value(0))
                .andExpect(jsonPath("$.honorarios.length()").value(1))
                .andExpect(jsonPath("$.resumo.totalProcessos").value(0))
                .andExpect(jsonPath("$.resumo.emAberto").value(80.0));
    }

    @Test
    @DisplayName("ordenar por uma propriedade que nao existe devolve 500, como antes")
    void ordenacaoPorPropriedadeInexistente() throws Exception {
        Identidade id = Identidade.nova();
        criarCliente(id, "Maria Souza");

        buscar(id, "/api/clientes?sort=inexistente,asc")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERRO_INTERNO"));
    }

    @Test
    @DisplayName("/internal/clientes/{id}/existe com orgId em branco responde falso")
    void existeInternoComEscritorioEmBranco() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");

        mvc.perform(get("/internal/clientes/{id}/existe", clienteId).param("orgId", "")
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false));
    }

    @Test
    @DisplayName("/internal/clientes/{id}/existe responde por escritorio")
    void existeInterno() throws Exception {
        Identidade id = Identidade.nova();
        String clienteId = criarCliente(id, "Helena Prado");

        mvc.perform(get("/internal/clientes/{id}/existe", clienteId).param("orgId", id.orgId())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true));
        mvc.perform(get("/internal/clientes/{id}/existe", clienteId).param("orgId", novoId())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(jsonPath("$.existe").value(false));
        mvc.perform(get("/internal/clientes/{id}/existe", novoId()).param("orgId", id.orgId())
                        .header("X-Internal-Key", CHAVE_INTERNA))
                .andExpect(jsonPath("$.existe").value(false));
    }
}
