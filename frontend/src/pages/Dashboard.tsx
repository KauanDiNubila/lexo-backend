import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Users, Briefcase, AlarmClock, Wallet, Sparkles, type LucideIcon } from "lucide-react";
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
  return new Date(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
}

type Metrica = {
  rotulo: string;
  valor: string | number;
  sub?: string;
  icone: LucideIcon;
  cor: string;
  grad: string;
  para: string;
};

function KpiCard({ m }: { m: Metrica }) {
  return (
    <Link
      to={m.para}
      className="card transition-transform hover:-translate-y-1"
      style={{
        position: "relative",
        overflow: "hidden",
        padding: "1.3rem",
        textDecoration: "none",
        color: "inherit",
        display: "block",
        boxShadow: `0 18px 40px -32px ${m.cor}`,
      }}
    >

      <div
        style={{
          position: "absolute",
          top: -40,
          right: -40,
          width: 130,
          height: 130,
          borderRadius: "50%",
          background: m.cor,
          opacity: 0.14,
          filter: "blur(28px)",
          pointerEvents: "none",
        }}
      />
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", position: "relative" }}>
        <span style={{ color: "var(--color-text-muted)", fontSize: 13, fontWeight: 600 }}>{m.rotulo}</span>
        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: 11,
            display: "grid",
            placeItems: "center",
            background: m.grad,
            boxShadow: `0 6px 18px -6px ${m.cor}`,
          }}
        >
          <m.icone size={20} color="white" strokeWidth={2.2} />
        </div>
      </div>
      <div
        style={{
          fontSize: 32,
          fontWeight: 800,
          marginTop: 12,
          background: m.grad,
          WebkitBackgroundClip: "text",
          backgroundClip: "text",
          color: "transparent",
          width: "fit-content",
        }}
      >
        {m.valor}
      </div>
      {m.sub && <div style={{ fontSize: 12, color: "var(--color-text-muted)", marginTop: 2 }}>{m.sub}</div>}
    </Link>
  );
}

function CardBase({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div className="card" style={{ padding: "1.35rem", boxShadow: "0 20px 45px -40px rgba(0,0,0,0.9)", ...style }}>
      {children}
    </div>
  );
}

function TituloSecao({ texto, link, para }: { texto: string; link: string; para: string }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
      <h2 style={{ fontSize: 15, fontWeight: 700, margin: 0, display: "flex", alignItems: "center", gap: 8 }}>
        <span style={{ width: 4, height: 16, borderRadius: 2, background: "linear-gradient(180deg,#818cf8,#8b5cf6)" }} />
        {texto}
      </h2>
      <Link to={para} style={{ fontSize: 13, color: "var(--color-primary)", textDecoration: "none" }}>{link} →</Link>
    </div>
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

  const metricas: Metrica[] = [
    { rotulo: "Clientes", valor: carregando ? "—" : clientes.length, icone: Users, cor: "#6366f1", grad: "linear-gradient(135deg,#818cf8,#6366f1)", para: "/app/clientes" },
    { rotulo: "Processos ativos", valor: carregando ? "—" : m.ativos, sub: `${processos.length} no total`, icone: Briefcase, cor: "#34d399", grad: "linear-gradient(135deg,#34d399,#10b981)", para: "/app/processos" },
    { rotulo: "Prazos urgentes", valor: carregando ? "—" : m.urgentes.length, sub: m.vencidos ? `${m.vencidos} vencido(s)` : "próximos 7 dias", icone: AlarmClock, cor: "#fbbf24", grad: "linear-gradient(135deg,#fbbf24,#f59e0b)", para: "/app/agenda" },
    { rotulo: "Em aberto", valor: carregando ? "—" : moeda(m.emAberto), sub: `recebido: ${moeda(m.recebido)}`, icone: Wallet, cor: "#a78bfa", grad: "linear-gradient(135deg,#c084fc,#8b5cf6)", para: "/app/financeiro" },
  ];

  return (
    <div>
      <h1 style={{ fontSize: 27, fontWeight: 800, margin: "0 0 4px", letterSpacing: -0.4 }}>
        Olá,{" "}
        <span style={{ background: "linear-gradient(135deg,#818cf8,#c084fc)", WebkitBackgroundClip: "text", backgroundClip: "text", color: "transparent" }}>
          {primeiroNome}
        </span>{" "}
        👋
      </h1>
      <p style={{ color: "var(--color-text-muted)", margin: "0 0 24px" }}>
        Visão geral do seu escritório — dados dos microserviços via API Gateway.
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(215px, 1fr))", gap: 16 }}>
        {metricas.map((mt) => (
          <KpiCard key={mt.rotulo} m={mt} />
        ))}
      </div>

      {!carregando && (
        <div
          className="card"
          style={{
            marginTop: 16,
            padding: "1.15rem 1.35rem",
            background: "linear-gradient(120deg, var(--color-primary-soft), var(--color-surface) 70%)",
            borderColor: "rgba(129,140,248,0.35)",
            boxShadow: "0 20px 50px -38px rgba(99,102,241,0.9)",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 5 }}>
            <span
              style={{
                width: 26, height: 26, borderRadius: 8, display: "grid", placeItems: "center",
                background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
              }}
            >
              <Sparkles size={15} color="white" strokeWidth={2.2} />
            </span>
            <span style={{ fontSize: 12.5, fontWeight: 700, color: "#c7d2fe", letterSpacing: 0.2 }}>Resumo do dia</span>
          </div>
          <p style={{ fontSize: 14.5, margin: 0, color: "var(--color-text)", lineHeight: 1.55 }}>
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

        <CardBase>
          <TituloSecao texto="Próximos prazos" link="Ver agenda" para="/app/agenda" />
          {carregando ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Carregando...</div>
          ) : m.proximos.length === 0 ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Nenhum prazo em aberto.</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column" }}>
              {m.proximos.map((p) => {
                const vencido = new Date(p.date).getTime() < Date.now();
                return (
                  <div key={p.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, padding: "0.65rem 0", borderTop: "1px solid var(--color-border)" }}>
                    <span style={{ display: "flex", alignItems: "center", gap: 10, overflow: "hidden" }}>
                      <span style={{ width: 8, height: 8, borderRadius: "50%", background: vencido ? "var(--color-danger)" : "var(--color-primary)", flexShrink: 0 }} />
                      <span style={{ fontSize: 14, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{p.title}</span>
                    </span>
                    <span style={{ fontSize: 13, fontWeight: 700, color: vencido ? "var(--color-danger)" : "var(--color-text-muted)", whiteSpace: "nowrap" }}>
                      {formatarData(p.date)}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </CardBase>

        <CardBase>
          <TituloSecao texto="Processos por status" link="Ver todos" para="/app/processos" />
          {carregando ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Carregando...</div>
          ) : processos.length === 0 ? (
            <div style={{ color: "var(--color-text-muted)", fontSize: 14 }}>Nenhum processo cadastrado.</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
              {["ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO"].map((s) => {
                const qtd = m.porStatus[s] || 0;
                const pct = processos.length ? (qtd / processos.length) * 100 : 0;
                const cor = corStatusProcesso[s];
                return (
                  <div key={s}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginBottom: 5 }}>
                      <span style={{ color: "var(--color-text-muted)", fontWeight: 600 }}>{s}</span>
                      <span style={{ fontWeight: 700 }}>{qtd}</span>
                    </div>
                    <div style={{ height: 8, borderRadius: 999, background: "var(--color-surface-2)", overflow: "hidden" }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: `linear-gradient(90deg, ${cor}99, ${cor})`, borderRadius: 999, transition: "width .4s ease" }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardBase>
      </div>
    </div>
  );
}
