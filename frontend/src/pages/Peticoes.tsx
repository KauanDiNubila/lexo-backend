import { useEffect, useState, type FormEvent } from "react";
import { Sparkles, Copy, Check } from "lucide-react";
import { api } from "../lib/api";
import { Markdown } from "../components/Markdown";

type Processo = { id: string; number: string; area: string | null; status: string; clientId: string };
type Cliente = { id: string; name: string };

const EXEMPLOS = [
  "Petição inicial de cobrança de honorários advocatícios",
  "Notificação extrajudicial de rescisão contratual",
  "Contestação em ação de cobrança",
];

export function Peticoes() {
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [pedido, setPedido] = useState("");
  const [processoId, setProcessoId] = useState("");
  const [gerando, setGerando] = useState(false);
  const [texto, setTexto] = useState("");
  const [fonte, setFonte] = useState("");
  const [copiado, setCopiado] = useState(false);

  useEffect(() => {
    Promise.all([
      api.get<Processo[]>("/api/processos").catch(() => [] as Processo[]),
      api.get<Cliente[]>("/api/clientes").catch(() => [] as Cliente[]),
    ]).then(([ps, cs]) => {
      setProcessos(ps);
      setClientes(cs);
    });
  }, []);

  const nomeCliente = (id: string) => clientes.find((c) => c.id === id)?.name || "—";

  async function gerar(e: FormEvent) {
    e.preventDefault();
    if (!pedido.trim() || gerando) return;
    setGerando(true);
    setTexto("");
    setFonte("");
    const p = processos.find((x) => x.id === processoId);
    const contexto = p
      ? { numero: p.number, area: p.area, status: p.status, cliente: nomeCliente(p.clientId) }
      : null;
    try {
      const r = await api.post<{ texto: string; fonte: string }>("/api/ia/rascunhar-peticao", {
        pedido: pedido.trim(),
        contexto,
      });
      setTexto(r.texto);
      setFonte(r.fonte);
    } catch {
      setTexto("Não foi possível gerar a minuta agora. Tente novamente em instantes.");
      setFonte("erro");
    } finally {
      setGerando(false);
    }
  }

  async function copiar() {
    await navigator.clipboard.writeText(texto);
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
  }

  return (
    <div style={{ maxWidth: 820, margin: "0 auto" }}>
      <div style={{ marginBottom: 18 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0, display: "flex", alignItems: "center", gap: 10 }}>
          <span className="grid h-8 w-8 place-items-center rounded-lg text-white" style={{ background: "linear-gradient(135deg,#6366f1,#8b5cf6)" }}>
            <Sparkles size={17} />
          </span>
          Gerar petição
        </h1>
        <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
          Descreva a peça e a Lexo IA redige uma minuta · via <code>ia-service</code>
        </p>
      </div>

      <form onSubmit={gerar} className="card" style={{ padding: "1.5rem", marginBottom: 18 }}>
        <label className="label">O que você quer gerar? *</label>
        <textarea
          className="input"
          value={pedido}
          onChange={(e) => setPedido(e.target.value)}
          placeholder="Ex.: petição inicial de cobrança de honorários não pagos"
          rows={3}
          style={{ resize: "vertical", fontFamily: "inherit" }}
          required
        />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", margin: "10px 0 4px" }}>
          {EXEMPLOS.map((ex) => (
            <button
              key={ex}
              type="button"
              onClick={() => setPedido(ex)}
              style={{ fontSize: 12, padding: "0.3rem 0.6rem", borderRadius: 999, cursor: "pointer", background: "var(--color-surface-2)", border: "1px solid var(--color-border)", color: "var(--color-text-muted)" }}
            >
              {ex}
            </button>
          ))}
        </div>

        <div style={{ marginTop: 14 }}>
          <label className="label">Vincular a um processo (opcional — dá contexto à IA)</label>
          <select className="input" value={processoId} onChange={(e) => setProcessoId(e.target.value)} style={{ maxWidth: 380 }}>
            <option value="">Nenhum</option>
            {processos.map((p) => (
              <option key={p.id} value={p.id}>{p.number} — {nomeCliente(p.clientId)}</option>
            ))}
          </select>
        </div>

        <div style={{ marginTop: 18 }}>
          <button type="submit" className="btn btn-primary" disabled={gerando || !pedido.trim()}>
            <Sparkles size={15} /> {gerando ? "Redigindo minuta..." : "Gerar minuta"}
          </button>
        </div>
      </form>

      {(texto || gerando) && (
        <div className="card" style={{ padding: "1.5rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <h2 style={{ fontSize: 15, fontWeight: 700, margin: 0 }}>Minuta gerada</h2>
            {texto && (
              <button className="btn btn-ghost" onClick={copiar} style={{ padding: "0.4rem 0.7rem", fontSize: 13 }}>
                {copiado ? <><Check size={14} /> Copiado</> : <><Copy size={14} /> Copiar</>}
              </button>
            )}
          </div>
          {gerando ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14, padding: "12px 0" }}>Redigindo a minuta...</div>
          ) : (
            <>
              <div
                style={{
                  fontSize: 14,
                  background: "var(--color-bg)",
                  border: "1px solid var(--color-border)",
                  borderRadius: 10,
                  padding: "1.1rem 1.25rem",
                  maxHeight: 520,
                  overflowY: "auto",
                }}
              >
                <Markdown>{texto}</Markdown>
              </div>
              <p style={{ fontSize: 12, color: "var(--color-text-muted)", marginTop: 10 }}>
                {fonte === "gemini" ? "✨ Gerado pela Lexo IA (Gemini)" : "Modelo automático · configure a Lexo IA para geração completa"}
                {" · "}Minuta a revisar pelo advogado responsável.
              </p>
            </>
          )}
        </div>
      )}
    </div>
  );
}
