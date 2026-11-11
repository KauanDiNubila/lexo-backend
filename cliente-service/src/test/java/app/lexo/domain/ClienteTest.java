package app.lexo.domain;

import app.lexo.common.erro.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cliente")
class ClienteTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final EscritorioId ESCRITORIO = new EscritorioId("escritorio-1");

    private DadosDoCliente dados(String nome) {
        return new DadosDoCliente(nome, null, "maria@escritorio.com", "11999990000", "obs");
    }

    @Test
    @DisplayName("nome com menos de 2 letras ou em branco e recusado")
    void nomeCurto() {
        assertThatThrownBy(() -> dados("A")).isInstanceOf(DadoInvalidoException.class).hasMessage("Nome muito curto");
        assertThatThrownBy(() -> dados("   ")).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> dados(null)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    @DisplayName("e-mail, telefone e observacoes em branco viram nulos")
    void camposEmBrancoViramNulos() {
        DadosDoCliente dados = new DadosDoCliente("Maria", null, " ", "", "  ");

        assertThat(dados.email()).isNull();
        assertThat(dados.telefone()).isNull();
        assertThat(dados.observacoes()).isNull();
    }

    @Test
    @DisplayName("cadastrar nasce sem id e sem link do portal")
    void cadastrar() {
        Cliente cliente = Cliente.cadastrar(ESCRITORIO, dados("Maria Souza"));

        assertThat(cliente.id()).isNull();
        assertThat(cliente.escritorioId()).isEqualTo(ESCRITORIO);
        assertThat(cliente.nome()).isEqualTo("Maria Souza");
        assertThat(cliente.linkDoPortal()).isNull();
        assertThat(cliente.portalAtivoEm(AGORA)).isFalse();
    }

    @Test
    @DisplayName("atualizar troca os dados e mantem o link do portal")
    void atualizar() {
        Cliente cliente = Cliente.cadastrar(ESCRITORIO, dados("Maria Souza"));
        cliente.gerarLinkDoPortal("abc", AGORA, 90);

        cliente.atualizar(dados("Maria Souza Lima"));

        assertThat(cliente.nome()).isEqualTo("Maria Souza Lima");
        assertThat(cliente.linkDoPortal().token()).isEqualTo("abc");
    }

    @Test
    @DisplayName("o link do portal vale pelos dias combinados e depois expira")
    void validadeDoLink() {
        Cliente cliente = Cliente.cadastrar(ESCRITORIO, dados("Maria Souza"));

        LinkDoPortal link = cliente.gerarLinkDoPortal("abc", AGORA, 90);

        assertThat(link.expiraEm()).isEqualTo(Instant.parse("2027-02-09T12:00:00Z"));
        assertThat(cliente.portalAtivoEm(AGORA.plusSeconds(60))).isTrue();
        assertThat(cliente.portalAtivoEm(Instant.parse("2027-02-09T11:59:59Z"))).isTrue();
        assertThat(cliente.portalAtivoEm(Instant.parse("2027-02-09T12:00:00Z"))).isFalse();
        assertThat(cliente.portalAtivoEm(Instant.parse("2027-03-01T00:00:00Z"))).isFalse();
    }

    @Test
    @DisplayName("gerar um novo link substitui o anterior")
    void novoLinkSubstitui() {
        Cliente cliente = Cliente.cadastrar(ESCRITORIO, dados("Maria Souza"));
        cliente.gerarLinkDoPortal("primeiro", AGORA, 90);

        cliente.gerarLinkDoPortal("segundo", AGORA, 90);

        assertThat(cliente.linkDoPortal().token()).isEqualTo("segundo");
    }

    @Test
    @DisplayName("revogar apaga o link")
    void revogar() {
        Cliente cliente = Cliente.cadastrar(ESCRITORIO, dados("Maria Souza"));
        cliente.gerarLinkDoPortal("abc", AGORA, 90);

        cliente.revogarLinkDoPortal();

        assertThat(cliente.linkDoPortal()).isNull();
        assertThat(cliente.portalAtivoEm(AGORA)).isFalse();
    }

    @Test
    @DisplayName("link sem validade nunca esta ativo")
    void linkSemValidade() {
        Cliente cliente = Cliente.reconstituir(new ClienteId("c1"), ESCRITORIO, dados("Maria Souza"),
                new LinkDoPortal("abc", null), AGORA);

        assertThat(cliente.portalAtivoEm(AGORA)).isFalse();
    }
}
