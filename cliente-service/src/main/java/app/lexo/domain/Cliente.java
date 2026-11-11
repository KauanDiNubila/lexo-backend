package app.lexo.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Cliente {

    private final ClienteId id;
    private final EscritorioId escritorioId;
    private final Instant criadoEm;
    private DadosDoCliente dados;
    private LinkDoPortal linkDoPortal;

    private Cliente(ClienteId id, EscritorioId escritorioId, DadosDoCliente dados, LinkDoPortal linkDoPortal,
                    Instant criadoEm) {
        this.id = id;
        this.escritorioId = escritorioId;
        this.dados = dados;
        this.linkDoPortal = linkDoPortal;
        this.criadoEm = criadoEm;
    }

    public static Cliente cadastrar(EscritorioId escritorioId, DadosDoCliente dados) {
        return new Cliente(null, escritorioId, dados, null, null);
    }

    public static Cliente reconstituir(ClienteId id, EscritorioId escritorioId, DadosDoCliente dados,
                                       LinkDoPortal linkDoPortal, Instant criadoEm) {
        return new Cliente(id, escritorioId, dados, linkDoPortal, criadoEm);
    }

    public void atualizar(DadosDoCliente novosDados) {
        this.dados = novosDados;
    }

    public LinkDoPortal gerarLinkDoPortal(String token, Instant agora, long validadeEmDias) {
        this.linkDoPortal = new LinkDoPortal(token, agora.plus(validadeEmDias, ChronoUnit.DAYS));
        return linkDoPortal;
    }

    public void revogarLinkDoPortal() {
        this.linkDoPortal = null;
    }

    public boolean portalAtivoEm(Instant agora) {
        return linkDoPortal != null && linkDoPortal.ativoEm(agora);
    }

    public ClienteId id() {
        return id;
    }

    public EscritorioId escritorioId() {
        return escritorioId;
    }

    public DadosDoCliente dados() {
        return dados;
    }

    public String nome() {
        return dados.nome();
    }

    public LinkDoPortal linkDoPortal() {
        return linkDoPortal;
    }

    public Instant criadoEm() {
        return criadoEm;
    }
}
