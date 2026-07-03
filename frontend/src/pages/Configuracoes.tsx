import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { api } from "../lib/api";

type OrganizacaoInfo = { id: string; name: string; plan: string; trialEndsAt: string | null; membros: number };
type PerfilInfo = { id: string; name: string; email: string; role: string; totpEnabled: boolean };
type Conta = { organizacao: OrganizacaoInfo; usuario: PerfilInfo };

type Aviso = { tipo: "ok" | "erro"; texto: string } | null;

const rotuloPlano: Record<string, string> = {
  trial: "Teste grátis",
  solo: "Solo",
  escritorio: "Escritório",
  enterprise: "Enterprise",
};

function Badge({ texto, cor }: { texto: string; cor: string }) {
  return (
    <span
      style={{
        fontSize: 12,
        fontWeight: 700,
        color: cor,
        background: "var(--color-surface-2)",
        padding: "0.2rem 0.6rem",
        borderRadius: 999,
      }}
    >
      {texto}
    </span>
  );
}

function CampoEditavel({
  valor,
  podeEditar,
  onSalvar,
}: {
  valor: string;
  podeEditar: boolean;
  onSalvar: (novo: string) => Promise<void>;
}) {
  const [editando, setEditando] = useState(false);
  const [texto, setTexto] = useState(valor);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => setTexto(valor), [valor]);

  if (!editando) {
    return (
      <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
        <span style={{ fontSize: 15, fontWeight: 600 }}>{valor}</span>
        {podeEditar && (
          <button
            onClick={() => setEditando(true)}
            style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontSize: 13 }}
          >
            Editar
          </button>
        )}
      </div>
    );
  }

  return (
    <div style={{ display: "flex", gap: 8, alignItems: "center", maxWidth: 420 }}>
      <input className="input" value={texto} onChange={(e) => setTexto(e.target.value)} autoFocus />
      <button
        className="btn btn-primary"
        disabled={salvando || texto.trim().length < 2}
        onClick={async () => {
          setSalvando(true);
          try {
            await onSalvar(texto.trim());
            setEditando(false);
          } finally {
            setSalvando(false);
          }
        }}
        style={{ padding: "0.5rem 0.8rem" }}
      >
        {salvando ? "..." : "Salvar"}
      </button>
      <button className="btn btn-ghost" onClick={() => { setEditando(false); setTexto(valor); }} style={{ padding: "0.5rem 0.8rem" }}>
        Cancelar
      </button>
    </div>
  );
}

function Secao({ titulo, descricao, children }: { titulo: string; descricao?: string; children: React.ReactNode }) {
  return (
    <div className="card" style={{ padding: "1.5rem", marginBottom: 18 }}>
      <h2 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 4px" }}>{titulo}</h2>
      {descricao && <p style={{ color: "var(--color-text-muted)", fontSize: 14, margin: "0 0 16px" }}>{descricao}</p>}
      {children}
    </div>
  );
}

function Linha({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16, padding: "0.7rem 0", borderTop: "1px solid var(--color-border)" }}>
      <span style={{ fontSize: 13, color: "var(--color-text-muted)", fontWeight: 600 }}>{rotulo}</span>
      <div style={{ textAlign: "right" }}>{children}</div>
    </div>
  );
}

export function Configuracoes() {
  const [conta, setConta] = useState<Conta | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [aviso, setAviso] = useState<Aviso>(null);

  const [segredo, setSegredo] = useState<string | null>(null);
  const [otpauth, setOtpauth] = useState<string>("");
  const [codigo, setCodigo] = useState("");
  const [codigoOff, setCodigoOff] = useState("");
  const [busy, setBusy] = useState(false);

  async function carregar() {
    try {
      setConta(await api.get<Conta>("/api/conta"));
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  function flash(a: Aviso) {
    setAviso(a);
    if (a) setTimeout(() => setAviso(null), 4000);
  }

  async function salvarOrg(novo: string) {
    try {
      const info = await api.put<OrganizacaoInfo>("/api/organizacao", { name: novo });
      setConta((c) => (c ? { ...c, organizacao: info } : c));
      flash({ tipo: "ok", texto: "Nome do escritório atualizado." });
    } catch (e) {
      flash({ tipo: "erro", texto: (e as Error).message });
      throw e;
    }
  }

  async function salvarPerfil(novo: string) {
    try {
      const info = await api.put<PerfilInfo>("/api/perfil", { name: novo });
      setConta((c) => (c ? { ...c, usuario: info } : c));
      flash({ tipo: "ok", texto: "Perfil atualizado. O nome no topo muda no próximo login." });
    } catch (e) {
      flash({ tipo: "erro", texto: (e as Error).message });
      throw e;
    }
  }

  async function iniciar2fa() {
    setBusy(true);
    try {
      const r = await api.post<{ secret: string; otpauthUri: string }>("/api/2fa/iniciar");
      setSegredo(r.secret);
      setOtpauth(r.otpauthUri);
    } catch (e) {
      flash({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  async function confirmar2fa() {
    setBusy(true);
    try {
      await api.post("/api/2fa/confirmar", { code: codigo });
      setSegredo(null);
      setOtpauth("");
      setCodigo("");
      setConta((c) => (c ? { ...c, usuario: { ...c.usuario, totpEnabled: true } } : c));
      flash({ tipo: "ok", texto: "2FA ativado com sucesso! 🎉" });
    } catch (e) {
      flash({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  async function desativar2fa() {
    setBusy(true);
    try {
      await api.post("/api/2fa/desativar", { code: codigoOff });
      setCodigoOff("");
      setConta((c) => (c ? { ...c, usuario: { ...c.usuario, totpEnabled: false } } : c));
      flash({ tipo: "ok", texto: "2FA desativado." });
    } catch (e) {
      flash({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  const corPapel: Record<string, string> = { ADMIN: "var(--color-primary)", ADVOGADO: "#34d399", SECRETARIA: "#fbbf24" };
  const ehAdmin = conta?.usuario.role === "ADMIN";

  return (
    <div style={{ maxWidth: 620 }}>
      <div style={{ marginBottom: 22 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Configurações</h1>
        <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
          Escritório, perfil e segurança · via <code>auth-service</code>
        </p>
      </div>

      {aviso && (
        <div
          style={{
            marginBottom: 18,
            fontSize: 14,
            padding: "0.7rem 1rem",
            borderRadius: 8,
            color: aviso.tipo === "ok" ? "#34d399" : "var(--color-danger)",
            background: aviso.tipo === "ok" ? "rgba(52,211,153,0.1)" : "rgba(248,113,113,0.1)",
            border: `1px solid ${aviso.tipo === "ok" ? "rgba(52,211,153,0.3)" : "rgba(248,113,113,0.3)"}`,
          }}
        >
          {aviso.texto}
        </div>
      )}

      {erro ? (
        <div className="card" style={{ padding: "1.5rem", color: "var(--color-danger)" }}>{erro}</div>
      ) : !conta ? (
        <div className="card" style={{ padding: "1.5rem", color: "var(--color-text-muted)" }}>Carregando...</div>
      ) : (
        <>

          <Secao titulo="Escritório" descricao={ehAdmin ? "Dados da sua organização." : "Apenas administradores podem editar o escritório."}>
            <Linha rotulo="Nome do escritório">
              <CampoEditavel valor={conta.organizacao.name} podeEditar={!!ehAdmin} onSalvar={salvarOrg} />
            </Linha>
            <Linha rotulo="Plano">
              <Badge texto={rotuloPlano[conta.organizacao.plan] || conta.organizacao.plan} cor="var(--color-primary)" />
            </Linha>
            <Linha rotulo="Membros">
              <span style={{ fontSize: 15, fontWeight: 600 }}>{conta.organizacao.membros}</span>
            </Linha>
            <Linha rotulo="ID da organização">
              <code style={{ fontSize: 12, color: "var(--color-text-muted)" }}>{conta.organizacao.id}</code>
            </Linha>
          </Secao>

          <Secao titulo="Meu perfil" descricao="Seus dados de acesso.">
            <Linha rotulo="Nome">
              <CampoEditavel valor={conta.usuario.name} podeEditar onSalvar={salvarPerfil} />
            </Linha>
            <Linha rotulo="Email">
              <span style={{ fontSize: 15, color: "var(--color-text-muted)" }}>{conta.usuario.email}</span>
            </Linha>
            <Linha rotulo="Papel">
              <Badge texto={conta.usuario.role} cor={corPapel[conta.usuario.role] || "var(--color-text-muted)"} />
            </Linha>
          </Secao>

          <Secao
            titulo="Verificação em dois fatores (2FA)"
            descricao="Camada extra de segurança com um app autenticador (Google Authenticator, Authy...)."
          >
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 16 }}>
              <span style={{ fontSize: 13, color: "var(--color-text-muted)", fontWeight: 600 }}>Status:</span>
              {conta.usuario.totpEnabled ? (
                <Badge texto="✓ Ativado" cor="#34d399" />
              ) : (
                <Badge texto="Desativado" cor="var(--color-text-muted)" />
              )}
            </div>

            {!conta.usuario.totpEnabled && !segredo && (
              <button className="btn btn-primary" onClick={iniciar2fa} disabled={busy}>
                {busy ? "Aguarde..." : "Configurar 2FA"}
              </button>
            )}

            {segredo && (
              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <div>
                  <label className="label">1. Escaneie o QR code no seu app autenticador:</label>
                  <div style={{ display: "flex", gap: 20, alignItems: "center", flexWrap: "wrap" }}>
                    <div style={{ background: "#fff", padding: 12, borderRadius: 12, lineHeight: 0 }}>
                      <QRCodeSVG value={otpauth} size={168} level="M" />
                    </div>
                    <div style={{ flex: 1, minWidth: 200 }}>
                      <div style={{ fontSize: 13, color: "var(--color-text-muted)", marginBottom: 6 }}>
                        Ou insira esta chave manualmente:
                      </div>
                      <div
                        style={{
                          fontFamily: "ui-monospace, monospace",
                          fontSize: 14,
                          letterSpacing: 1.5,
                          background: "var(--color-bg)",
                          border: "1px solid var(--color-border)",
                          borderRadius: 8,
                          padding: "0.6rem 0.8rem",
                          wordBreak: "break-all",
                        }}
                      >
                        {segredo}
                      </div>
                    </div>
                  </div>
                </div>
                <div>
                  <label className="label">2. Digite o código de 6 dígitos gerado:</label>
                  <input
                    className="input"
                    style={{ maxWidth: 200 }}
                    value={codigo}
                    onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ""))}
                    placeholder="000000"
                    maxLength={6}
                    inputMode="numeric"
                  />
                </div>
                <div style={{ display: "flex", gap: 10 }}>
                  <button className="btn btn-primary" onClick={confirmar2fa} disabled={busy || codigo.length < 6}>
                    Confirmar e ativar
                  </button>
                  <button className="btn btn-ghost" onClick={() => { setSegredo(null); setOtpauth(""); setCodigo(""); }}>
                    Cancelar
                  </button>
                </div>
              </div>
            )}

            {conta.usuario.totpEnabled && (
              <div>
                <label className="label">Informe um código atual do app para desativar:</label>
                <div style={{ display: "flex", gap: 10 }}>
                  <input
                    className="input"
                    style={{ maxWidth: 200 }}
                    value={codigoOff}
                    onChange={(e) => setCodigoOff(e.target.value.replace(/\D/g, ""))}
                    placeholder="000000"
                    maxLength={6}
                    inputMode="numeric"
                  />
                  <button className="btn btn-ghost" onClick={desativar2fa} disabled={busy || codigoOff.length < 6}>
                    Desativar
                  </button>
                </div>
              </div>
            )}
          </Secao>
        </>
      )}
    </div>
  );
}
