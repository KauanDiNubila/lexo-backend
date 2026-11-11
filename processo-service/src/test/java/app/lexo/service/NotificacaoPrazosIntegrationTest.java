package app.lexo.service;

import app.lexo.IntegracaoBase;
import app.lexo.common.email.TarefaEmail;
import app.lexo.common.usuario.UsuarioInterno;
import app.lexo.repository.DeadlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Lembrete de prazos (integracao)")
class NotificacaoPrazosIntegrationTest extends IntegracaoBase {

    @Autowired
    private NotificacaoPrazosService servico;

    @Autowired
    private DeadlineRepository prazos;

    private ValueOperations<String, String> valores;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void lockLivre() {
        valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        when(valores.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    private String processoComResponsavel(Identidade id, String responsavelId, String numero) throws Exception {
        Map<String, Object> dados = new HashMap<>(dadosDoProcesso(novoId(), numero));
        if (responsavelId != null) {
            dados.put("responsavelId", responsavelId);
        }
        return idDe(postar(id, "/api/processos", dados).andExpect(status().isCreated()));
    }

    private void responsavelComEmail(String responsavelId, String email) {
        when(authClient.buscarUsuario(eq(responsavelId), anyString()))
                .thenReturn(new UsuarioInterno(responsavelId, "Ana Advogada", email));
    }

    private void verificarEnvios(String email, int vezes) {
        verify(this.email, times(vezes)).enfileirar(argThat((TarefaEmail t) -> email.equals(t.destinatario())));
    }

    @Test
    @DisplayName("avisa o responsavel dos prazos pendentes nos proximos 3 dias e nao repete")
    void avisaUmaVez() throws Exception {
        Identidade id = Identidade.nova();
        String responsavelId = novoId();
        String endereco = UUID.randomUUID() + "@escritorio.com";
        responsavelComEmail(responsavelId, endereco);
        String processoId = processoComResponsavel(id, responsavelId, "NT-001");
        String dentro = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(1));
        String distante = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(10));
        String passado = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(-1));
        String concluido = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(2));
        alternarStatus(id, concluido, true).andExpect(status().isOk());

        servico.executar();

        verificarEnvios(endereco, 1);
        verify(email).enfileirar(argThat((TarefaEmail t) -> endereco.equals(t.destinatario())
                && "LEMBRETE_PRAZO".equals(t.tipo()) && t.assunto().startsWith("Prazo próximo: ")));
        assertThat(prazos.findByIdAndOrganizationId(dentro, id.orgId()).orElseThrow().getNotifiedAt()).isNotNull();
        assertThat(prazos.findByIdAndOrganizationId(distante, id.orgId()).orElseThrow().getNotifiedAt()).isNull();
        assertThat(prazos.findByIdAndOrganizationId(passado, id.orgId()).orElseThrow().getNotifiedAt()).isNull();
        assertThat(prazos.findByIdAndOrganizationId(concluido, id.orgId()).orElseThrow().getNotifiedAt()).isNull();

        servico.executar();

        verificarEnvios(endereco, 1);
    }

    @Test
    @DisplayName("pula prazo sem responsavel e responsavel sem e-mail, sem marcar como avisado")
    void pulaSemDestinatario() throws Exception {
        Identidade id = Identidade.nova();
        String semEmail = novoId();
        responsavelComEmail(semEmail, "");
        String processoSemResponsavel = processoComResponsavel(id, null, "NT-002");
        String processoSemEmail = processoComResponsavel(id, semEmail, "NT-003");
        String prazoA = criarPrazo(id, processoSemResponsavel, "PRAZO", Duration.ofDays(1));
        String prazoB = criarPrazo(id, processoSemEmail, "PRAZO", Duration.ofDays(1));

        servico.executar();

        verificarEnvios("", 0);
        assertThat(prazos.findByIdAndOrganizationId(prazoA, id.orgId()).orElseThrow().getNotifiedAt()).isNull();
        assertThat(prazos.findByIdAndOrganizationId(prazoB, id.orgId()).orElseThrow().getNotifiedAt()).isNull();
    }

    @Test
    @DisplayName("com o lock ocupado por outra instancia, nao faz nada")
    void lockOcupado() throws Exception {
        Identidade id = Identidade.nova();
        String responsavelId = novoId();
        responsavelComEmail(responsavelId, UUID.randomUUID() + "@escritorio.com");
        String processoId = processoComResponsavel(id, responsavelId, "NT-004");
        String prazoId = criarPrazo(id, processoId, "PRAZO", Duration.ofDays(1));
        when(valores.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        int enviados = servico.executar();

        assertThat(enviados).isZero();
        verifyNoInteractions(email);
        verify(authClient, never()).buscarUsuario(anyString(), anyString());
        assertThat(prazos.findByIdAndOrganizationId(prazoId, id.orgId()).orElseThrow().getNotifiedAt()).isNull();
    }
}
