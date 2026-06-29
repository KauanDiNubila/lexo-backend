import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";

type Prazo = {
  id: string;
  caseId: string;
  title: string;
  type: string;
  status: string;
  date: string;
  description: string | null;
  risk: string | null;
};

type Processo = { id: string; number: string };

const TIPOS = ["PRAZO", "AUDIENCIA", "REUNIAO", "OUTRO"];

const corRisco: Record<string, string> = {
  CRITICO: "#f87171",
  URGENTE: "#fb923c",
  ALTO: "#fbbf24",
  MEDIO: "#60a5fa",
  BAIXO: "#34d399",
};

export function Agenda() {
  const [prazos, setPrazos] = useState<Prazo[]>([]);
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aberto, setAberto] = useState(false);

  const [caseId, setCaseId] = useState("");
  const [titulo, setTitulo] = useState("");
  const [tipo, setTipo] = useState("PRAZO");
  const [data, setData] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [ps, procs] = await Promise.all([
        api.get<Prazo[]>("/api/agenda"),
        api.get<Processo[]>("/api/processos"),
      ]);
      setPrazos(ps);
      setProcessos(procs);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  const numProcesso = (id: string) => processos.find((p) => p.id === id)?.number || "—";

  async function criar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    try {
      await api.post("/api/agenda", {
        caseId,
        title: titulo,
        type: tipo,
        date: `${data}T00:00:00Z`,
        description: null,
      });
      setCaseId("");
      setTitulo("");
      setTipo("PRAZO");
      setData("");
      setAberto(false);
      await carregar();
    } catch (e) {
      setErroForm((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  async function alternar(id: string, concluido: boolean) {
    await api.patch(`/api/agenda/${id}/status`, { completed: concluido });
    await carregar();
  }

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Agenda</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {prazos.length} compromisso(s) · via <code>processo-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setAberto(true)}>+ Novo prazo</button>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : prazos.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Nenhum prazo ainda.</div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>TÍTULO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>PROCESSO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>TIPO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>DATA</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>RISCO</th>
                <th style={{ padding: "0.85rem 1.25rem" }}></th>
              </tr>
            </thead>
            <tbody>
              {prazos.map((p) => (
                <tr key={p.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600, textDecoration: p.status === "CONCLUIDO" ? "line-through" : "none", opacity: p.status === "CONCLUIDO" ? 0.6 : 1 }}>{p.title}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{numProcesso(p.caseId)}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{p.type}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>
                    {new Date(p.date).toLocaleDateString("pt-BR", { timeZone: "UTC" })}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    {p.risk ? (
                      <span style={{ fontSize: 12, fontWeight: 700, color: corRisco[p.risk] || "#9aa3b8" }}>{p.risk}</span>
                    ) : (
                      <span style={{ color: "var(--color-text-muted)" }}>—</span>
                    )}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem", textAlign: "right" }}>
                    <button
                      onClick={() => alternar(p.id, p.status !== "CONCLUIDO")}
                      style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontSize: 13 }}
                    >
                      {p.status === "CONCLUIDO" ? "Reabrir" : "Concluir"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {aberto && (
        <div onClick={() => setAberto(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}>
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 440, maxWidth: "100%", padding: "1.75rem" }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>Novo prazo</h2>
            <form onSubmit={criar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Processo *</label>
                <select className="input" value={caseId} onChange={(e) => setCaseId(e.target.value)} required>
                  <option value="">Selecione...</option>
                  {processos.map((p) => (
                    <option key={p.id} value={p.id}>{p.number}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Título *</label>
                <input className="input" value={titulo} onChange={(e) => setTitulo(e.target.value)} required placeholder="Contestação" />
              </div>
              <div>
                <label className="label">Tipo</label>
                <select className="input" value={tipo} onChange={(e) => setTipo(e.target.value)}>
                  {TIPOS.map((t) => (<option key={t} value={t}>{t}</option>))}
                </select>
              </div>
              <div>
                <label className="label">Data *</label>
                <input className="input" type="date" value={data} onChange={(e) => setData(e.target.value)} required />
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>{salvando ? "Salvando..." : "Salvar"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
