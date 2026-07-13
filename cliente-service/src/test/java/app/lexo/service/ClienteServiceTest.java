package app.lexo.service;

import app.lexo.controller.ApiException;
import app.lexo.domain.Client;
import app.lexo.domain.enums.Role;
import app.lexo.dto.ClientDtos.ClientRequest;
import app.lexo.dto.ClientDtos.ClientResponse;
import app.lexo.evento.EventoDominio;
import app.lexo.evento.PublicadorEventos;
import app.lexo.repository.ClientRepository;
import app.lexo.security.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService (unitario)")
class ClienteServiceTest {

    private static final String ORG = "org-123";
    private static final String OUTRA_ORG = "org-999";
    private static final String CPF_VALIDO = "52998224725";

    @Mock
    private ClientRepository repo;

    @Mock
    private PublicadorEventos eventos;

    @InjectMocks
    private ClienteService service;

    private AuthUser usuario(String org) {
        return new AuthUser("user-1", org, "Ana Advogada", "ana@escritorio.com", Role.ADMIN);
    }

    private Client cliente(String id, String org, String nome) {
        Client c = new Client();
        c.setId(id);
        c.setOrganizationId(org);
        c.setName(nome);
        return c;
    }

    private ClientRequest requisicao(String nome, String documento) {
        return new ClientRequest(nome, documento, null, null, null);
    }

    @Test
    @DisplayName("listar consulta o repositorio pela organizacao do usuario")
    void listarFiltraPelaOrganizacao() {
        when(repo.findByOrganizationIdOrderByNameAsc(ORG))
                .thenReturn(List.of(cliente("c1", ORG, "Bruno"), cliente("c2", ORG, "Carla")));

        List<ClientResponse> resultado = service.listar(usuario(ORG));

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ClientResponse::name).containsExactly("Bruno", "Carla");
        verify(repo).findByOrganizationIdOrderByNameAsc(ORG);
    }

    @Test
    @DisplayName("buscar retorna o cliente quando ele pertence a organizacao")
    void buscarQuandoExiste() {
        when(repo.findByIdAndOrganizationId("c1", ORG))
                .thenReturn(Optional.of(cliente("c1", ORG, "Bruno")));

        ClientResponse resposta = service.buscar(usuario(ORG), "c1");

        assertThat(resposta.id()).isEqualTo("c1");
        assertThat(resposta.name()).isEqualTo("Bruno");
    }

    @Test
    @DisplayName("buscar lanca 404 quando o cliente nao existe na organizacao")
    void buscarQuandoNaoExisteLanca404() {
        when(repo.findByIdAndOrganizationId("c1", ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(usuario(ORG), "c1"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("criar carimba a organizacao, salva e publica evento de dominio")
    void criarCarimbaOrgEPublicaEvento() {
        when(repo.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            c.setId("novo-id");
            return c;
        });

        ClientResponse resposta = service.criar(usuario(ORG), requisicao("Novo Cliente", CPF_VALIDO));

        assertThat(resposta.id()).isEqualTo("novo-id");
        assertThat(resposta.name()).isEqualTo("Novo Cliente");

        ArgumentCaptor<Client> salvo = ArgumentCaptor.forClass(Client.class);
        verify(repo).save(salvo.capture());
        assertThat(salvo.getValue().getOrganizationId()).isEqualTo(ORG);

        ArgumentCaptor<EventoDominio> evento = ArgumentCaptor.forClass(EventoDominio.class);
        verify(eventos).publicar(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo("CLIENTE_CRIADO");
        assertThat(evento.getValue().organizationId()).isEqualTo(ORG);
        assertThat(evento.getValue().entityId()).isEqualTo("novo-id");
    }

    @Test
    @DisplayName("criar com CPF invalido lanca 400 e nao salva nem publica evento")
    void criarComDocumentoInvalidoNaoSalva() {
        assertThatThrownBy(() -> service.criar(usuario(ORG), requisicao("Fulano", "11111111111")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(repo, never()).save(any());
        verify(eventos, never()).publicar(any());
    }

    @Test
    @DisplayName("criar sem documento e permitido (validacao so roda com documento preenchido)")
    void criarSemDocumentoOk() {
        when(repo.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            c.setId("id-2");
            return c;
        });

        ClientResponse resposta = service.criar(usuario(ORG), requisicao("Sem Documento", null));

        assertThat(resposta.id()).isEqualTo("id-2");
        verify(repo).save(any(Client.class));
    }

    @Test
    @DisplayName("atualizar altera os campos quando o cliente existe na organizacao")
    void atualizarQuandoExiste() {
        Client existente = cliente("c1", ORG, "Nome Antigo");
        when(repo.findByIdAndOrganizationId("c1", ORG)).thenReturn(Optional.of(existente));
        when(repo.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse resposta = service.atualizar(usuario(ORG), "c1", requisicao("Nome Novo", CPF_VALIDO));

        assertThat(resposta.name()).isEqualTo("Nome Novo");
        assertThat(existente.getName()).isEqualTo("Nome Novo");
    }

    @Test
    @DisplayName("atualizar lanca 404 quando o cliente nao existe na organizacao")
    void atualizarQuandoNaoExisteLanca404() {
        when(repo.findByIdAndOrganizationId("c1", ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(usuario(ORG), "c1", requisicao("X", CPF_VALIDO)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("excluir remove sempre restrito a organizacao do usuario (multi-tenant)")
    void excluirRestringePelaOrganizacao() {
        service.excluir(usuario(OUTRA_ORG), "c1");

        verify(repo).deleteByIdAndOrganizationId(eq("c1"), eq(OUTRA_ORG));
    }
}
