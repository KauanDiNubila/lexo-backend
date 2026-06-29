import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";

type Processo = {
  id: string;
  clientId: string;
  number: string;
  area: string | null;
  status: string;
};

type Cliente = { id: string; name: string };

const STATUS = ["ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO"];

const corStatus: Record<string, string> = {
  ATIVO: "#34d399",
  SUSPENSO: "#fbbf24",
  ARQUIVADO: "#9aa3b8",
  ENCERRADO: "#f87171",
};

export function Processos() {
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aberto, setAberto] = useState(false);

  const [clientId, setClientId] = useState("");
  const [numero, setNumero] = useState("");
  const [area, setArea] = useState("");
  const [status, setStatus] = useState("ATIVO");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [ps, cs] = await Promise.all([
        api.get<Processo[]>("/api/processos"),
        api.get<Cliente[]>("/api/clientes"),
      ]);
      setProcessos(ps);
      setClientes(cs);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  const nomeCliente = (id: string) => clientes.find((c) => c.id === id)?.name || "—";

  async function criar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    try {
      await api.post("/api/processos", { clientId, number: numero, area: area || null, status });
      setClientId("");
      setNumero("");
      setArea("");
      setStatus("ATIVO");
      setAberto(false);
      await carregar();
    } catch (e) {
      setErroForm((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Processos</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {processos.length} processo(s) · via <code>processo-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setAberto(true)}>
          + Novo processo
        </button>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : processos.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>
            Nenhum processo ainda.
          </div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>NÚMERO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>CLIENTE</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>ÁREA</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {processos.map((p) => (
                <tr key={p.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>{p.number}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{nomeCliente(p.clientId)}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{p.area || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    <span
                      style={{
                        fontSize: 12,
                        fontWeight: 700,
                        color: corStatus[p.status] || "#9aa3b8",
                        background: "var(--color-surface-2)",
                        padding: "0.2rem 0.6rem",
                        borderRadius: 999,
                      }}
                    >
                      {p.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {aberto && (
        <div
          onClick={() => setAberto(false)}
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}
        >
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 440, maxWidth: "100%", padding: "1.75rem" }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>Novo processo</h2>
            <form onSubmit={criar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Cliente *</label>
                <select className="input" value={clientId} onChange={(e) => setClientId(e.target.value)} required>
                  <option value="">Selecione...</option>
                  {clientes.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Número do processo *</label>
                <input className="input" value={numero} onChange={(e) => setNumero(e.target.value)} required placeholder="0001-23.2026" />
              </div>
              <div>
                <label className="label">Área</label>
                <input className="input" value={area} onChange={(e) => setArea(e.target.value)} placeholder="Cível" />
              </div>
              <div>
                <label className="label">Status</label>
                <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
                  {STATUS.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>
                  {salvando ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
