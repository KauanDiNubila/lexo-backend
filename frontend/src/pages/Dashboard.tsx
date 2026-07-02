import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

type Cliente = { id: string };
type Processo = { id: string; number: string; status: string };
type Prazo = { id: string; title: string; status: string; date: string; risk: string | null };
type Honorario = { id: string; amount: number; status: string };

const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v);

const corStatusProcesso: Record<string, string> = {
  ATIVO: "#34d399",
  SUSPENSO: "#fbbf24",
  ARQUIVADO: "#9aa3b8",
  ENCERRADO: "#f87171",
};

const DIA = 86_400_000;

function formatarData(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
}

function KpiCard({ rotulo, valor, sub, cor, icone, para }: { rotulo: string; valor: string | number; sub?: string; cor: string; icone: string; para: string }) {
  return (
    <Link to={para} className="card" style={{ padding: "1.25rem", textDecoration: "none", color: "inherit", display: "block" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <span style={{ color: "var(--color-text-muted)", fontSize: 13, fontWeight: 600 }}>{rotulo}</span>
        <span style={{ fontSize: 20 }}>{icone}</span>
      </div>
      <div style={{ fontSize: 30, fontWeight: 800, marginTop: 8, color: cor }}>{valor}</div>
      {sub && <div style={{ fontSize: 12, color: "var(--color-text-muted)", marginTop: 2 }}>{sub}</div>}
    </Link>
  );
}

export function Dashboard() {
  const { usuario } = useAuth();
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [prazos, setPrazos] = useState<Prazo[]>([]);
  const [honorarios, setHonorarios] = useState<Honorario[]>([]);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      api.get<Cliente[]>("/api/clientes"),
      api.get<Processo[]>("/api/processos"),
      api.get<Prazo[]>("/api/agenda"),
      api.get<Honorario[]>("/api/financeiro"),
    ]).then(([c, p, a, f]) => {
      if (c.status === "fulfilled") setClientes(c.value);
      if (p.status === "fulfilled") setProcessos(p.value);
      if (a.status === "fulfilled") setPrazos(a.value);
      if (f.status === "fulfilled") setHonorarios(f.value);
      setCarregando(false);
    });
  }, []);

  const m = useMemo(() => {
    const agora = Date.now();
    const em7dias = agora + 7 * DIA;
    const abertos = prazos.filter((p) => p.status !== "CONCLUIDO");
    const urgentes = abertos.filter((p) => {
      const t = new Date(p.date).getTime();
      return !Number.isNaN(t) && t <= em7dias;
    });
    const proximos = [...abertos]
      .filter((p) => !Number.isNaN(new Date(p.date).getTime()))
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
      .slice(0, 5);

    const emAberto = honorarios
      .filter((h) => h.status === "PENDENTE" || h.status === "ATRASADO")
      .reduce((s, h) => s + h.amount, 0);
    const recebido = honorarios.filter((h) => h.status === "PAGO").reduce((s, h) => s + h.amount, 0);

    const porStatus = processos.reduce<Record<string, number>>((acc, p) => {
      acc[p.status] = (acc[p.status] || 0) + 1;
      return acc;
    }, {});

    return {
      ativos: processos.filter((p) => p.status === "ATIVO").length,
      urgentes,
      proximos,
      emAberto,
      recebido,
      porStatus,
      vencidos: abertos.filter((p) => new Date(p.date).getTime() < agora).length,
    };
  }, [processos, prazos, honorarios]);

  const primeiroNome = usuario?.name?.split(" ")[0];

  return (
    <div>
      <h1 style={{ fontSize: 26, fontWeight: 700, margin: "0 0 4px" }}>Olá, {primeiroNome} 👋</h1>
      <p style={{ color: "var(--color-text-muted)", margin: "0 0 24px" }}>
        Visão geral do seu escritório — dados dos microserviços via API Gateway.
      </p>

      {/* KPIs */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: 16 }}>
        <KpiCard rotulo="Clientes" valor={carregando ? "—" : clientes.length} cor="#6366f1" icone="👥" para="/app/clientes" />
        <KpiCard rotulo="Processos ativos" valor={carregando ? "—" : m.ativos} sub={`${processos.length} no total`} cor="#34d399" icone="📂" para="/app/processos" />
        <KpiCard rotulo="Prazos urgentes" valor={carregando ? "—" : m.urgentes.length} sub={m.vencidos ? `${m.vencidos} vencido(s)` : "próximos 7 dias"} cor="#fbbf24" icone="⏰" para="/app/agenda" />
        <KpiCard rotulo="Em aberto" valor={carregando ? "—" : moeda(m.emAberto)} sub={`recebido: ${moeda(m.recebido)}`} cor="#818cf8" icone="💰" para="/app/financeiro" />
      </div>

      {/* Resumo estilo IA */}
      {!carregando && (
        <div className="card" style={{ marginTop: 16, padding: "1.1rem 1.25rem", background: "var(--color-primary-soft)" }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: "var(--color-primary)", marginBottom: 4 }}>🤖 Resumo do dia</div>
          <p style={{ fontSize: 14, margin: 0, color: "var(--color-text)" }}>
            {m.urgentes.length > 0 ? (
              <>
                Você tem <b>{m.urgentes.length} prazo(s)</b> nos próximos 7 dias
                {m.vencidos > 0 && <> (<b style={{ color: "var(--color-danger)" }}>{m.vencidos} já vencido(s)</b>)</>}.{" "}
              </>
            ) : (
              <>Nenhum prazo urgente nos próximos 7 dias. </>
            )}
            {m.emAberto > 0
              ? <>Há <b>{moeda(m.emAberto)}</b> em honorários a receber.</>
              : <>Nenhum honorário em aberto no momento.</>}
          </p>
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 16, marginTop: 16 }}>
        {/* Próximos prazos */}
        <div className="card" style={{ padding: "1.25rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
            <h2 style={{ fontSize: 15, fontWeight: 700, margin: 0 }}>Próximos prazos</h2>
            <Link to="/app/agenda" style={{ fontSize: 13, color: "var(--color-primary)", textDecoration: "none" }}>Ver agenda →</Link>
          </div>
          {carregando ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Carregando...</div>
          ) : m.proximos.length === 0 ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Nenhum prazo em aberto. 🎉</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column" }}>
              {m.proximos.map((p) => {
                const vencido = new Date(p.date).getTime() < Date.now();
                return (
                  <div key={p.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, padding: "0.6rem 0", borderTop: "1px solid var(--color-border)" }}>
                    <span style={{ fontSize: 14, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.title}</span>
                    <span style={{ fontSize: 13, fontWeight: 700, color: vencido ? "var(--color-danger)" : "var(--color-text-muted)", whiteSpace: "nowrap" }}>
                      {formatarData(p.date)}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Processos por status */}
        <div className="card" style={{ padding: "1.25rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
            <h2 style={{ fontSize: 15, fontWeight: 700, margin: 0 }}>Processos por status</h2>
            <Link to="/app/processos" style={{ fontSize: 13, color: "var(--color-primary)", textDecoration: "none" }}>Ver todos →</Link>
          </div>
          {carregando ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Carregando...</div>
          ) : processos.length === 0 ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Nenhum processo cadastrado.</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              {["ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO"].map((s) => {
                const qtd = m.porStatus[s] || 0;
                const pct = processos.length ? (qtd / processos.length) * 100 : 0;
                return (
                  <div key={s}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginBottom: 4 }}>
                      <span style={{ color: "var(--color-text-muted)", fontWeight: 600 }}>{s}</span>
                      <span style={{ fontWeight: 700 }}>{qtd}</span>
                    </div>
                    <div style={{ height: 7, borderRadius: 999, background: "var(--color-surface-2)", overflow: "hidden" }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: corStatusProcesso[s], borderRadius: 999, transition: "width .3s" }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
