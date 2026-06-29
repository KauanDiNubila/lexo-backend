import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";

type Honorario = {
  id: string;
  clientId: string;
  caseId: string | null;
  description: string;
  amount: number;
  status: string;
  dueDate: string;
};

type Cliente = { id: string; name: string };
type Processo = { id: string; number: string };

const STATUS = ["PENDENTE", "PAGO", "ATRASADO", "CANCELADO"];

const corStatus: Record<string, string> = {
  PAGO: "#34d399",
  PENDENTE: "#fbbf24",
  ATRASADO: "#f87171",
  CANCELADO: "#9aa3b8",
};

const moeda = (v: number) =>
  v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function Financeiro() {
  const [itens, setItens] = useState<Honorario[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aberto, setAberto] = useState(false);

  const [clientId, setClientId] = useState("");
  const [caseId, setCaseId] = useState("");
  const [descricao, setDescricao] = useState("");
  const [valor, setValor] = useState("");
  const [status, setStatus] = useState("PENDENTE");
  const [vencimento, setVencimento] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [hs, cs, ps] = await Promise.all([
        api.get<Honorario[]>("/api/financeiro"),
        api.get<Cliente[]>("/api/clientes"),
        api.get<Processo[]>("/api/processos"),
      ]);
      setItens(hs);
      setClientes(cs);
      setProcessos(ps);
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
      await api.post("/api/financeiro", {
        clientId,
        caseId: caseId || null,
        description: descricao,
        amount: Number(valor),
        status,
        dueDate: `${vencimento}T00:00:00Z`,
      });
      setClientId("");
      setCaseId("");
      setDescricao("");
      setValor("");
      setStatus("PENDENTE");
      setVencimento("");
      setAberto(false);
      await carregar();
    } catch (e) {
      setErroForm((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  const total = itens.reduce((s, i) => s + (i.status === "PAGO" ? i.amount : 0), 0);

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Financeiro</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {itens.length} honorário(s) · recebido: <strong style={{ color: "#34d399" }}>{moeda(total)}</strong> · via <code>financeiro-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setAberto(true)}>+ Novo honorário</button>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : itens.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Nenhum honorário ainda.</div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>DESCRIÇÃO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>CLIENTE</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>VALOR</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>VENCIMENTO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((i) => (
                <tr key={i.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>{i.description}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{nomeCliente(i.clientId)}</td>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>{moeda(i.amount)}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>
                    {new Date(i.dueDate).toLocaleDateString("pt-BR", { timeZone: "UTC" })}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: corStatus[i.status] || "#9aa3b8", background: "var(--color-surface-2)", padding: "0.2rem 0.6rem", borderRadius: 999 }}>
                      {i.status}
                    </span>
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
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>Novo honorário</h2>
            <form onSubmit={criar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Cliente *</label>
                <select className="input" value={clientId} onChange={(e) => setClientId(e.target.value)} required>
                  <option value="">Selecione...</option>
                  {clientes.map((c) => (<option key={c.id} value={c.id}>{c.name}</option>))}
                </select>
              </div>
              <div>
                <label className="label">Processo (opcional)</label>
                <select className="input" value={caseId} onChange={(e) => setCaseId(e.target.value)}>
                  <option value="">Nenhum</option>
                  {processos.map((p) => (<option key={p.id} value={p.id}>{p.number}</option>))}
                </select>
              </div>
              <div>
                <label className="label">Descrição *</label>
                <input className="input" value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
              </div>
              <div style={{ display: "flex", gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <label className="label">Valor (R$) *</label>
                  <input className="input" type="number" step="0.01" min="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
                </div>
                <div style={{ flex: 1 }}>
                  <label className="label">Vencimento *</label>
                  <input className="input" type="date" value={vencimento} onChange={(e) => setVencimento(e.target.value)} required />
                </div>
              </div>
              <div>
                <label className="label">Status</label>
                <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
                  {STATUS.map((s) => (<option key={s} value={s}>{s}</option>))}
                </select>
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
