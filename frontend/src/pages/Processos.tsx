import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Sparkles } from "lucide-react";
import { api } from "../lib/api";
import { Markdown } from "../components/Markdown";

type Prazo = { id: string; caseId: string; title: string; status: string; date: string };

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

const FORM_VAZIO = { clientId: "", numero: "", area: "", status: "ATIVO" };

export function Processos() {
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [busca, setBusca] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");

  const [aberto, setAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  // Resumo por IA
  const [iaAberto, setIaAberto] = useState(false);
  const [iaProcesso, setIaProcesso] = useState<Processo | null>(null);
  const [iaCarregando, setIaCarregando] = useState(false);
  const [iaResumo, setIaResumo] = useState("");
  const [iaFonte, setIaFonte] = useState<string>("");

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

  const filtrados = useMemo(() => {
    const q = busca.trim().toLowerCase();
    return processos.filter((p) => {
      if (filtroStatus && p.status !== filtroStatus) return false;
      if (!q) return true;
      return [p.number, p.area, nomeCliente(p.clientId)].some((v) => v?.toLowerCase().includes(q));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [processos, clientes, busca, filtroStatus]);

  function abrirNovo() {
    setEditandoId(null);
    setForm(FORM_VAZIO);
    setErroForm(null);
    setAberto(true);
  }

  function abrirEdicao(p: Processo) {
    setEditandoId(p.id);
    setForm({ clientId: p.clientId, numero: p.number, area: p.area ?? "", status: p.status });
    setErroForm(null);
    setAberto(true);
  }

  async function salvar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    const corpo = { clientId: form.clientId, number: form.numero, area: form.area || null, status: form.status };
    try {
      if (editandoId) {
        await api.put(`/api/processos/${editandoId}`, corpo);
      } else {
        await api.post("/api/processos", corpo);
      }
      setAberto(false);
      await carregar();
    } catch (e) {
      setErroForm((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  async function excluir(id: string) {
    if (!confirm("Excluir este processo?")) return;
    await api.del(`/api/processos/${id}`);
    await carregar();
  }

  async function resumirIA(p: Processo) {
    setIaProcesso(p);
    setIaAberto(true);
    setIaCarregando(true);
    setIaResumo("");
    setIaFonte("");
    try {
      // reúne os prazos deste processo para dar contexto à IA
      const agenda = await api.get<Prazo[]>("/api/agenda").catch(() => [] as Prazo[]);
      const prazos = agenda
        .filter((d) => d.caseId === p.id)
        .map((d) => ({
          titulo: d.title,
          data: new Date(d.date).toLocaleDateString("pt-BR", { timeZone: "UTC" }),
          status: d.status,
        }));
      const r = await api.post<{ resumo: string; fonte: string }>("/api/ia/resumir-processo", {
        numero: p.number,
        area: p.area,
        status: p.status,
        cliente: nomeCliente(p.clientId),
        prazos,
      });
      setIaResumo(r.resumo);
      setIaFonte(r.fonte);
    } catch (e) {
      setIaResumo("Não foi possível gerar o resumo agora. Tente novamente.");
      setIaFonte("erro");
    } finally {
      setIaCarregando(false);
    }
  }

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Processos</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {busca || filtroStatus ? `${filtrados.length} de ${processos.length}` : `${processos.length} processo(s)`} · via{" "}
            <code>processo-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo processo
        </button>
      </div>

      <div style={{ display: "flex", gap: 10, marginBottom: 14, flexWrap: "wrap" }}>
        <input
          className="input"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar por número, cliente ou área..."
          style={{ maxWidth: 360 }}
        />
        <select className="input" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)} style={{ maxWidth: 180 }}>
          <option value="">Todos os status</option>
          {STATUS.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : filtrados.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>
            {processos.length === 0 ? "Nenhum processo ainda." : "Nenhum processo corresponde ao filtro."}
          </div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>NÚMERO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>CLIENTE</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>ÁREA</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>STATUS</th>
                <th style={{ padding: "0.85rem 1.25rem" }}></th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((p) => (
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
                  <td style={{ padding: "0.85rem 1.25rem", textAlign: "right", whiteSpace: "nowrap" }}>
                    <button
                      onClick={() => resumirIA(p)}
                      title="Resumir com a Lexo IA"
                      style={{ background: "none", border: "none", color: "#c084fc", cursor: "pointer", fontSize: 13, marginRight: 14, display: "inline-flex", alignItems: "center", gap: 4, verticalAlign: "middle" }}
                    >
                      <Sparkles size={13} /> Resumir
                    </button>
                    <button
                      onClick={() => abrirEdicao(p)}
                      style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontSize: 13, marginRight: 14 }}
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => excluir(p.id)}
                      style={{ background: "none", border: "none", color: "var(--color-danger)", cursor: "pointer", fontSize: 13 }}
                    >
                      Excluir
                    </button>
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
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>
              {editandoId ? "Editar processo" : "Novo processo"}
            </h2>
            <form onSubmit={salvar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Cliente *</label>
                <select className="input" value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })} required>
                  <option value="">Selecione...</option>
                  {clientes.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Número do processo *</label>
                <input className="input" value={form.numero} onChange={(e) => setForm({ ...form, numero: e.target.value })} required placeholder="0001-23.2026" />
              </div>
              <div>
                <label className="label">Área</label>
                <input className="input" value={form.area} onChange={(e) => setForm({ ...form, area: e.target.value })} placeholder="Cível" />
              </div>
              <div>
                <label className="label">Status</label>
                <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  {STATUS.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>
                  {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal de resumo por IA */}
      {iaAberto && (
        <div
          onClick={() => setIaAberto(false)}
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}
        >
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 480, maxWidth: "100%", padding: "1.75rem" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
              <span className="grid h-7 w-7 place-items-center rounded-lg text-white" style={{ background: "linear-gradient(135deg,#6366f1,#8b5cf6)" }}>
                <Sparkles size={15} />
              </span>
              <h2 style={{ fontSize: 17, fontWeight: 700, margin: 0 }}>Resumo do processo</h2>
            </div>
            <p style={{ fontSize: 13, color: "var(--color-text-muted)", margin: "0 0 16px" }}>
              {iaProcesso?.number}
            </p>

            {iaCarregando ? (
              <div style={{ padding: "20px 0", color: "var(--color-text-muted)", fontSize: 14 }}>
                Analisando o processo...
              </div>
            ) : (
              <>
                <div style={{ fontSize: 14.5 }}><Markdown>{iaResumo}</Markdown></div>
                {iaFonte && iaFonte !== "erro" && (
                  <div style={{ marginTop: 14, fontSize: 12, color: "var(--color-text-muted)" }}>
                    {iaFonte === "gemini"
                      ? "✨ Gerado pela Lexo IA (Gemini)"
                      : "Resumo automático · configure a Lexo IA (GEMINI_API_KEY) para análises mais ricas"}
                  </div>
                )}
              </>
            )}

            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 20 }}>
              <button className="btn btn-ghost" onClick={() => setIaAberto(false)}>Fechar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
