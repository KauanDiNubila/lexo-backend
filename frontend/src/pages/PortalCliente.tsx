import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Briefcase, CalendarClock, Wallet, History, Lock, type LucideIcon } from "lucide-react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

const BASE = import.meta.env.VITE_GATEWAY_URL || "http://localhost:8090";

type PrazoPortal = { title: string; type: string; status: string; date: string };
type AndamentoPortal = { title: string; description: string | null; date: string };
type ProcessoPortal = { number: string; area: string | null; status: string; createdAt: string; prazos: PrazoPortal[]; andamentos: AndamentoPortal[] };
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
  ATIVO: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  SUSPENSO: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
  ARQUIVADO: "bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
  ENCERRADO: "bg-red-500/10 text-red-700 dark:text-red-400",
};
const corHonorario: Record<string, string> = {
  PAGO: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  PENDENTE: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
  ATRASADO: "bg-red-500/10 text-red-700 dark:text-red-400",
  CANCELADO: "bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
};

function Badge({ texto, className }: { texto: string; className?: string }) {
  return (
    <span className={cn("shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold", className)}>
      {texto}
    </span>
  );
}

function IconeChip({ icone: Icone }: { icone: LucideIcon }) {
  return (
    <div className="grid size-11 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">
      <Icone size={20} />
    </div>
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
    <div className="min-h-screen bg-background">
      <header className="border-b border-border">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
          <span className="text-lg font-bold tracking-tight">Lexo</span>
          <span className="text-xs text-muted-foreground">Portal do cliente</span>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        {carregando ? (
          <Card className="items-center px-6 py-16 text-center text-sm text-muted-foreground">Carregando…</Card>
        ) : erro ? (
          <Card className="items-center px-6 py-16 text-center">
            <div className="mx-auto grid size-12 place-items-center rounded-full bg-muted text-muted-foreground">
              <Lock size={20} />
            </div>
            <h1 className="mt-4 text-lg font-semibold">Link inválido</h1>
            <p className="mt-1 text-sm text-muted-foreground">{erro}</p>
          </Card>
        ) : portal ? (
          <>
            <h1 className="text-2xl font-bold tracking-tight">Olá, {portal.cliente}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Acompanhe aqui o andamento dos seus processos e sua situação financeira, em tempo real.
            </p>

            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Card className="flex-row items-center gap-4 px-5 py-5">
                <IconeChip icone={Briefcase} />
                <div>
                  <div className="text-2xl font-bold">{portal.resumo.totalProcessos}</div>
                  <div className="text-sm text-muted-foreground">processo(s)</div>
                </div>
              </Card>
              <Card className="flex-row items-center gap-4 px-5 py-5">
                <IconeChip icone={Wallet} />
                <div>
                  <div className="text-2xl font-bold">{moeda(portal.resumo.emAberto)}</div>
                  <div className="text-sm text-muted-foreground">em aberto</div>
                </div>
              </Card>
            </div>

            <h2 className="mt-10 mb-3 text-base font-semibold">Seus processos</h2>
            {portal.processos.length === 0 ? (
              <Card className="px-6 py-6 text-sm text-muted-foreground">Nenhum processo cadastrado ainda.</Card>
            ) : (
              <div className="flex flex-col gap-3">
                {portal.processos.map((p, i) => (
                  <Card key={i} className="px-5 py-5">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <div className="text-[15px] font-semibold">{p.number}</div>
                        <div className="text-sm text-muted-foreground">{p.area || "Área não informada"}</div>
                      </div>
                      <Badge texto={p.status} className={corProcesso[p.status] || corProcesso.ARQUIVADO} />
                    </div>
                    {p.prazos.length > 0 && (
                      <div className="mt-3 border-t border-border pt-3">
                        <div className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
                          <CalendarClock size={13} /> Próximos compromissos
                        </div>
                        {p.prazos.map((pr, j) => (
                          <div key={j} className="flex justify-between py-1 text-sm">
                            <span>{pr.title}</span>
                            <span className="font-semibold text-muted-foreground">{data(pr.date)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                    {p.andamentos && p.andamentos.length > 0 && (
                      <div className="mt-3 border-t border-border pt-3">
                        <div className="mb-2 flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
                          <History size={13} /> Andamentos
                        </div>
                        {p.andamentos.map((a, j) => (
                          <div key={j} className="flex gap-2.5">
                            <div className="flex flex-col items-center">
                              <span className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" />
                              {j < p.andamentos.length - 1 && <span className="w-px flex-1 bg-border" />}
                            </div>
                            <div className="flex-1 pb-3">
                              <div className="flex justify-between gap-2 text-[13.5px]">
                                <span className="font-semibold">{a.title}</span>
                                <span className="whitespace-nowrap text-muted-foreground">{data(a.date)}</span>
                              </div>
                              {a.description && <div className="mt-0.5 text-sm text-muted-foreground">{a.description}</div>}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </Card>
                ))}
              </div>
            )}

            <h2 className="mt-10 mb-3 text-base font-semibold">Financeiro</h2>
            {portal.honorarios.length === 0 ? (
              <Card className="px-6 py-6 text-sm text-muted-foreground">Nenhum lançamento financeiro.</Card>
            ) : (
              <Card className="gap-0 py-0">
                {portal.honorarios.map((h, i) => (
                  <div
                    key={i}
                    className={cn(
                      "flex items-center justify-between gap-3 px-5 py-3.5",
                      i > 0 && "border-t border-border"
                    )}
                  >
                    <div>
                      <div className="text-sm font-semibold">{h.description}</div>
                      <div className="text-xs text-muted-foreground">vence em {data(h.dueDate)}</div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold">{moeda(h.amount)}</span>
                      <Badge texto={h.status} className={corHonorario[h.status] || corHonorario.CANCELADO} />
                    </div>
                  </div>
                ))}
              </Card>
            )}

            <p className="mt-8 text-center text-xs text-muted-foreground">
              Dúvidas? Fale com seu advogado. · Powered by Lexo
            </p>
          </>
        ) : null}
      </main>
    </div>
  );
}
