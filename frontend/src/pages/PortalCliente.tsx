import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Scale, Briefcase, CalendarClock, Wallet } from "lucide-react";

const BASE = import.meta.env.VITE_GATEWAY_URL || "http://localhost:8080";

type PrazoPortal = { title: string; type: string; status: string; date: string };
type ProcessoPortal = { number: string; area: string | null; status: string; createdAt: string; prazos: PrazoPortal[] };
type HonorarioPortal = { description: string; amount: number; status: string; dueDate: string };
type Portal = {
  cliente: string;
  processos: ProcessoPortal[];
  honorarios: HonorarioPortal[];
  resumo: { totalProcessos: number; emAberto: number };
};

const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v);
const data = (iso: string) => new Date(iso).toLocaleDateString("pt-BR", { timeZone: "UTC" });

const corProcesso: Record<string, string> = {
  ATIVO: "#34d399", SUSPENSO: "#fbbf24", ARQUIVADO: "#9aa3b8", ENCERRADO: "#f87171",
};
const corHonorario: Record<string, string> = {
  PAGO: "#34d399", PENDENTE: "#fbbf24", ATRASADO: "#f87171", CANCELADO: "#9aa3b8",
};

function Marca() {
  return (
    <div className="flex items-center gap-2.5">
      <div className="grid h-9 w-9 place-items-center rounded-[10px] text-white" style={{ background: "linear-gradient(135deg,#6366f1,#8b5cf6)" }}>
        <Scale size={20} strokeWidth={2.3} />
      </div>
      <span className="text-xl font-bold tracking-tight">Lexo</span>
    </div>
  );
}

function Badge({ texto, cor }: { texto: string; cor: string }) {
  return (
    <span style={{ fontSize: 12, fontWeight: 700, color: cor, background: "var(--color-surface-2)", padding: "0.2rem 0.6rem", borderRadius: 999 }}>
      {texto}
    </span>
  );
}

export function PortalCliente() {
  const { token } = useParams();
  const [portal, setPortal] = useState<Portal | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    fetch(`${BASE}/api/portal/${token}`)
      .then(async (r) => {
        const d = await r.json().catch(() => null);
        if (!r.ok) throw new Error((d && d.error) || "Não foi possível carregar o portal.");
        return d as Portal;
      })
      .then(setPortal)
      .catch((e) => setErro((e as Error).message))
      .finally(() => setCarregando(false));
  }, [token]);

  return (
    <div style={{ minHeight: "100vh", background: "var(--color-bg)" }}>
      <header style={{ borderBottom: "1px solid var(--color-border)" }}>
        <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
          <Marca />
          <span className="text-xs" style={{ color: "var(--color-text-muted)" }}>Portal do cliente</span>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        {carregando ? (
          <div className="card" style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div className="card" style={{ padding: 40, textAlign: "center" }}>
            <div style={{ fontSize: 40, marginBottom: 8 }}>🔒</div>
            <h1 style={{ fontSize: 20, fontWeight: 700, margin: "0 0 6px" }}>Link inválido</h1>
            <p style={{ color: "var(--color-text-muted)", margin: 0 }}>{erro}</p>
          </div>
        ) : portal ? (
          <>
            <h1 style={{ fontSize: 26, fontWeight: 800, margin: "0 0 4px", letterSpacing: -0.4 }}>
              Olá, {portal.cliente} 👋
            </h1>
            <p style={{ color: "var(--color-text-muted)", margin: "0 0 24px" }}>
              Acompanhe aqui o andamento dos seus processos e sua situação financeira, em tempo real.
            </p>

            {/* Resumo */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 16, marginBottom: 24 }}>
              <div className="card" style={{ padding: "1.1rem 1.25rem", display: "flex", alignItems: "center", gap: 14 }}>
                <div className="grid h-11 w-11 place-items-center rounded-xl text-white" style={{ background: "linear-gradient(135deg,#34d399,#10b981)" }}>
                  <Briefcase size={20} />
                </div>
                <div>
                  <div style={{ fontSize: 24, fontWeight: 800 }}>{portal.resumo.totalProcessos}</div>
                  <div style={{ fontSize: 13, color: "var(--color-text-muted)" }}>processo(s)</div>
                </div>
              </div>
              <div className="card" style={{ padding: "1.1rem 1.25rem", display: "flex", alignItems: "center", gap: 14 }}>
                <div className="grid h-11 w-11 place-items-center rounded-xl text-white" style={{ background: "linear-gradient(135deg,#c084fc,#8b5cf6)" }}>
                  <Wallet size={20} />
                </div>
                <div>
                  <div style={{ fontSize: 24, fontWeight: 800 }}>{moeda(portal.resumo.emAberto)}</div>
                  <div style={{ fontSize: 13, color: "var(--color-text-muted)" }}>em aberto</div>
                </div>
              </div>
            </div>

            {/* Processos */}
            <h2 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 12px" }}>Seus processos</h2>
            {portal.processos.length === 0 ? (
              <div className="card" style={{ padding: 24, color: "var(--color-text-muted)", marginBottom: 24 }}>Nenhum processo cadastrado ainda.</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 12, marginBottom: 28 }}>
                {portal.processos.map((p, i) => (
                  <div key={i} className="card" style={{ padding: "1.1rem 1.25rem" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
                      <div>
                        <div style={{ fontSize: 15, fontWeight: 700 }}>{p.number}</div>
                        <div style={{ fontSize: 13, color: "var(--color-text-muted)" }}>{p.area || "Área não informada"}</div>
                      </div>
                      <Badge texto={p.status} cor={corProcesso[p.status] || "#9aa3b8"} />
                    </div>
                    {p.prazos.length > 0 && (
                      <div style={{ marginTop: 12, borderTop: "1px solid var(--color-border)", paddingTop: 10 }}>
                        <div style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-muted)", marginBottom: 6, display: "flex", alignItems: "center", gap: 6 }}>
                          <CalendarClock size={13} /> Próximos compromissos
                        </div>
                        {p.prazos.map((pr, j) => (
                          <div key={j} style={{ display: "flex", justifyContent: "space-between", fontSize: 13, padding: "3px 0" }}>
                            <span>{pr.title}</span>
                            <span style={{ color: "var(--color-text-muted)", fontWeight: 600 }}>{data(pr.date)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* Honorários */}
            <h2 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 12px" }}>Financeiro</h2>
            {portal.honorarios.length === 0 ? (
              <div className="card" style={{ padding: 24, color: "var(--color-text-muted)" }}>Nenhum lançamento financeiro.</div>
            ) : (
              <div className="card" style={{ overflow: "hidden" }}>
                {portal.honorarios.map((h, i) => (
                  <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, padding: "0.85rem 1.25rem", borderTop: i ? "1px solid var(--color-border)" : "none" }}>
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 600 }}>{h.description}</div>
                      <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>vence em {data(h.dueDate)}</div>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <span style={{ fontSize: 14, fontWeight: 700 }}>{moeda(h.amount)}</span>
                      <Badge texto={h.status} cor={corHonorario[h.status] || "#9aa3b8"} />
                    </div>
                  </div>
                ))}
              </div>
            )}

            <p style={{ textAlign: "center", fontSize: 12, color: "var(--color-text-muted)", marginTop: 32 }}>
              Dúvidas? Fale com seu advogado. · Powered by Lexo
            </p>
          </>
        ) : null}
      </main>
    </div>
  );
}
