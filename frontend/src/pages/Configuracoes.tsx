import { useState } from "react";
import { api } from "../lib/api";

export function Configuracoes() {
  const [segredo, setSegredo] = useState<string | null>(null);
  const [codigo, setCodigo] = useState("");
  const [msg, setMsg] = useState<{ tipo: "ok" | "erro"; texto: string } | null>(null);
  const [busy, setBusy] = useState(false);

  const [codigoOff, setCodigoOff] = useState("");

  async function iniciar() {
    setMsg(null);
    setBusy(true);
    try {
      const r = await api.post<{ secret: string; otpauthUri: string }>("/api/2fa/iniciar");
      setSegredo(r.secret);
    } catch (e) {
      setMsg({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  async function confirmar() {
    setMsg(null);
    setBusy(true);
    try {
      await api.post("/api/2fa/confirmar", { code: codigo });
      setSegredo(null);
      setCodigo("");
      setMsg({ tipo: "ok", texto: "2FA ativado com sucesso! 🎉" });
    } catch (e) {
      setMsg({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  async function desativar() {
    setMsg(null);
    setBusy(true);
    try {
      await api.post("/api/2fa/desativar", { code: codigoOff });
      setCodigoOff("");
      setMsg({ tipo: "ok", texto: "2FA desativado." });
    } catch (e) {
      setMsg({ tipo: "erro", texto: (e as Error).message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ maxWidth: 560 }}>
      <div style={{ marginBottom: 22 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Configurações</h1>
        <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
          Segurança da conta · via <code>auth-service</code>
        </p>
      </div>

      {msg && (
        <div
          style={{
            marginBottom: 18,
            fontSize: 14,
            padding: "0.7rem 1rem",
            borderRadius: 8,
            color: msg.tipo === "ok" ? "#34d399" : "var(--color-danger)",
            background: msg.tipo === "ok" ? "rgba(52,211,153,0.1)" : "rgba(248,113,113,0.1)",
            border: `1px solid ${msg.tipo === "ok" ? "rgba(52,211,153,0.3)" : "rgba(248,113,113,0.3)"}`,
          }}
        >
          {msg.texto}
        </div>
      )}

      {/* Ativar 2FA */}
      <div className="card" style={{ padding: "1.5rem", marginBottom: 18 }}>
        <h2 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>Verificação em dois fatores (2FA)</h2>
        <p style={{ color: "var(--color-text-muted)", fontSize: 14, margin: "0 0 16px" }}>
          Adicione uma camada extra de segurança com um app autenticador (Google Authenticator, Authy...).
        </p>

        {!segredo ? (
          <button className="btn btn-primary" onClick={iniciar} disabled={busy}>
            {busy ? "Aguarde..." : "Configurar 2FA"}
          </button>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <div>
              <label className="label">1. Adicione esta chave no seu app autenticador (entrada manual):</label>
              <div
                style={{
                  fontFamily: "ui-monospace, monospace",
                  fontSize: 16,
                  letterSpacing: 2,
                  background: "var(--color-bg)",
                  border: "1px solid var(--color-border)",
                  borderRadius: 8,
                  padding: "0.7rem 1rem",
                  wordBreak: "break-all",
                }}
              >
                {segredo}
              </div>
            </div>
            <div>
              <label className="label">2. Digite o código de 6 dígitos gerado:</label>
              <input className="input" value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="000000" maxLength={6} />
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <button className="btn btn-primary" onClick={confirmar} disabled={busy || codigo.length < 6}>
                Confirmar e ativar
              </button>
              <button className="btn btn-ghost" onClick={() => { setSegredo(null); setCodigo(""); }}>
                Cancelar
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Desativar 2FA */}
      <div className="card" style={{ padding: "1.5rem" }}>
        <h2 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>Desativar 2FA</h2>
        <p style={{ color: "var(--color-text-muted)", fontSize: 14, margin: "0 0 14px" }}>
          Informe um código atual do app para desativar.
        </p>
        <div style={{ display: "flex", gap: 10 }}>
          <input className="input" style={{ maxWidth: 180 }} value={codigoOff} onChange={(e) => setCodigoOff(e.target.value)} placeholder="000000" maxLength={6} />
          <button className="btn btn-ghost" onClick={desativar} disabled={busy || codigoOff.length < 6}>
            Desativar
          </button>
        </div>
      </div>
    </div>
  );
}
