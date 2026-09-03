import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Users, Briefcase, AlarmClock, Wallet, Sparkles, type LucideIcon } from "lucide-react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type Prazo = { id: string; title: string; status: string; date: string; risk: string | null };

const moeda = (v: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(v);

const corStatusProcesso: Record<string, string> = {
  ATIVO: "bg-emerald-500",
  SUSPENSO: "bg-amber-500",
  ARQUIVADO: "bg-zinc-500",
  ENCERRADO: "bg-red-500",
};

function formatarData(iso: string) {
  return new Date(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
}

type Metrica = {
  rotulo: string;
  valor: string | number;
  sub?: string;
  icone: LucideIcon;
  para: string;
};

function KpiCard({ m }: { m: Metrica }) {
  return (
    <Link to={m.para}>
      <Card className="gap-2.5 p-5 transition-colors hover:bg-muted/40">
        <div className="flex items-center justify-between">
          <span className="text-[13px] font-semibold text-muted-foreground">{m.rotulo}</span>
          <div className="grid size-9 shrink-0 place-items-center rounded-[10px] bg-primary/10 text-primary">
            <m.icone size={18} strokeWidth={2.2} />
          </div>
        </div>
        <div className="text-[28px] font-bold tracking-tight">{m.valor}</div>
        {m.sub && <div className="text-xs text-muted-foreground">{m.sub}</div>}
      </Card>
    </Link>
  );
}

function TituloSecao({ texto, link, para }: { texto: string; link: string; para: string }) {
  return (
    <div className="mb-4 flex items-center justify-between">
      <h2 className="flex items-center gap-2 text-[15px] font-bold">
        <span className="h-4 w-1 rounded-sm bg-primary" />
        {texto}
      </h2>
      <Link to={para} className="text-[13px] text-primary hover:underline">
        {link} →
      </Link>
    </div>
  );
}

export function Dashboard() {
  const { usuario } = useAuth();
  const [m, setM] = useState({
    totalClientes: 0,
    totalProcessos: 0,
    ativos: 0,
    porStatus: {} as Record<string, number>,
    urgentes: 0,
    vencidos: 0,
    proximos: [] as Prazo[],
    emAberto: 0,
    recebido: 0,
  });
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      api.get<{ total: number }>("/api/clientes/resumo"),
      api.get<{ total: number; ativos: number; porStatus: Record<string, number> }>("/api/processos/resumo"),
      api.get<{ urgentes: number; vencidos: number; proximos: Prazo[] }>("/api/agenda/resumo"),
      api.get<{ emAberto: number; recebido: number }>("/api/financeiro/resumo"),
    ]).then(([c, p, a, f]) => {
      setM((atual) => ({
        ...atual,
        totalClientes: c.status === "fulfilled" ? c.value.total : 0,
        totalProcessos: p.status === "fulfilled" ? p.value.total : 0,
        ativos: p.status === "fulfilled" ? p.value.ativos : 0,
        porStatus: p.status === "fulfilled" ? p.value.porStatus : {},
        urgentes: a.status === "fulfilled" ? a.value.urgentes : 0,
        vencidos: a.status === "fulfilled" ? a.value.vencidos : 0,
        proximos: a.status === "fulfilled" ? a.value.proximos : [],
        emAberto: f.status === "fulfilled" ? f.value.emAberto : 0,
        recebido: f.status === "fulfilled" ? f.value.recebido : 0,
      }));
      setCarregando(false);
    });
  }, []);

  const primeiroNome = usuario?.name?.split(" ")[0];

  const metricas: Metrica[] = [
    { rotulo: "Clientes", valor: carregando ? "—" : m.totalClientes, icone: Users, para: "/app/clientes" },
    { rotulo: "Processos ativos", valor: carregando ? "—" : m.ativos, sub: `${m.totalProcessos} no total`, icone: Briefcase, para: "/app/processos" },
    { rotulo: "Prazos urgentes", valor: carregando ? "—" : m.urgentes, sub: m.vencidos ? `${m.vencidos} vencido(s)` : "próximos 7 dias", icone: AlarmClock, para: "/app/agenda" },
    { rotulo: "Em aberto", valor: carregando ? "—" : moeda(m.emAberto), sub: `recebido: ${moeda(m.recebido)}`, icone: Wallet, para: "/app/financeiro" },
  ];

  return (
    <div>
      <h1 className="text-[26px] font-bold tracking-tight">Olá, {primeiroNome}</h1>
      <p className="mt-1 mb-6 text-muted-foreground">
        Visão geral do seu escritório — dados dos microserviços via API Gateway.
      </p>

      <div className="grid grid-cols-[repeat(auto-fit,minmax(215px,1fr))] gap-4">
        {metricas.map((mt) => (
          <KpiCard key={mt.rotulo} m={mt} />
        ))}
      </div>

      {!carregando && (
        <Card className="mt-4 gap-1.5 p-5">
          <div className="flex items-center gap-2">
            <span className="grid size-6 shrink-0 place-items-center rounded-md bg-primary/10 text-primary">
              <Sparkles size={13} strokeWidth={2.2} />
            </span>
            <span className="text-xs font-bold tracking-wide text-muted-foreground">RESUMO DO DIA</span>
          </div>
          <p className="text-[14.5px] leading-relaxed">
            {m.urgentes > 0 ? (
              <>
                Você tem <b>{m.urgentes} prazo(s)</b> nos próximos 7 dias
                {m.vencidos > 0 && (
                  <>
                    {" "}(<b className="text-destructive">{m.vencidos} já vencido(s)</b>)
                  </>
                )}
                .{" "}
              </>
            ) : (
              <>Nenhum prazo urgente nos próximos 7 dias. </>
            )}
            {m.emAberto > 0 ? (
              <>
                Há <b>{moeda(m.emAberto)}</b> em honorários a receber.
              </>
            ) : (
              <>Nenhum honorário em aberto no momento.</>
            )}
          </p>
        </Card>
      )}

      <div className="mt-4 grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-4">
        <Card className="p-5">
          <TituloSecao texto="Próximos prazos" link="Ver agenda" para="/app/agenda" />
          {carregando ? (
            <div className="text-sm text-muted-foreground">Carregando...</div>
          ) : m.proximos.length === 0 ? (
            <div className="text-sm text-muted-foreground">Nenhum prazo em aberto.</div>
          ) : (
            <div className="flex flex-col">
              {m.proximos.map((p) => {
                const vencido = new Date(p.date).getTime() < Date.now();
                return (
                  <div
                    key={p.id}
                    className="flex items-center justify-between gap-3 border-t border-border py-2.5 first:border-t-0"
                  >
                    <span className="flex items-center gap-2.5 overflow-hidden">
                      <span
                        className={cn(
                          "size-2 shrink-0 rounded-full",
                          vencido ? "bg-destructive" : "bg-primary"
                        )}
                      />
                      <span className="truncate text-sm font-semibold">{p.title}</span>
                    </span>
                    <span
                      className={cn(
                        "whitespace-nowrap text-[13px] font-bold",
                        vencido ? "text-destructive" : "text-muted-foreground"
                      )}
                    >
                      {formatarData(p.date)}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <TituloSecao texto="Processos por status" link="Ver todos" para="/app/processos" />
          {carregando ? (
            <div className="text-sm text-muted-foreground">Carregando...</div>
          ) : m.totalProcessos === 0 ? (
            <div className="text-sm text-muted-foreground">Nenhum processo cadastrado.</div>
          ) : (
            <div className="flex flex-col gap-3.5">
              {["ATIVO", "SUSPENSO", "ARQUIVADO", "ENCERRADO"].map((s) => {
                const qtd = m.porStatus[s] || 0;
                const pct = m.totalProcessos ? (qtd / m.totalProcessos) * 100 : 0;
                return (
                  <div key={s}>
                    <div className="mb-1.5 flex justify-between text-xs">
                      <span className="font-semibold text-muted-foreground">{s}</span>
                      <span className="font-bold">{qtd}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-muted">
                      <div
                        className={cn("h-full rounded-full transition-[width] duration-300", corStatusProcesso[s])}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
