import { useEffect, useState, type FormEvent } from "react";
import { api, type Pagina } from "../lib/api";
import { Paginacao } from "../components/Paginacao";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

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

type Relatorio = {
  start: string;
  end: string;
  totalPago: number;
  totalPendente: number;
  totalAtrasado: number;
};

const STATUS = ["PENDENTE", "PAGO", "ATRASADO", "CANCELADO"];

const corStatus: Record<string, string> = {
  PAGO: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  PENDENTE: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
  ATRASADO: "bg-red-500/10 text-red-700 dark:text-red-400",
  CANCELADO: "bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
};

const selectClass =
  "flex h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

const moeda = (v: number) =>
  v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const iso = (d: Date) => d.toISOString().slice(0, 10);

function mesAtual() {
  const hoje = new Date();
  return {
    inicio: iso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)),
    fim: iso(new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0)),
  };
}

export function Financeiro() {
  const [itens, setItens] = useState<Honorario[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalRegistros, setTotalRegistros] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aberto, setAberto] = useState(false);

  const [clientId, setClientId] = useState("");
  const [caseId, setCaseId] = useState("");
  const [descricao, setDescricao] = useState("");
  const [valorCentavos, setValorCentavos] = useState(0);
  const [status, setStatus] = useState("PENDENTE");
  const [vencimento, setVencimento] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  const [periodo, setPeriodo] = useState(mesAtual);
  const [relatorio, setRelatorio] = useState<Relatorio | null>(null);
  const [carregandoRelatorio, setCarregandoRelatorio] = useState(true);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [hs, cs, ps] = await Promise.all([
        api.get<Pagina<Honorario>>(`/api/financeiro?page=${pagina}`),
        api.get<Cliente[]>("/api/clientes/opcoes"),
        api.get<Processo[]>("/api/processos/opcoes"),
      ]);
      setItens(hs.content);
      setTotalPaginas(hs.totalPages);
      setTotalRegistros(hs.totalElements);
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
  }, [pagina]);

  useEffect(() => {
    if (!periodo.inicio || !periodo.fim || periodo.inicio > periodo.fim) return;
    let ativo = true;
    setCarregandoRelatorio(true);
    api
      .get<Relatorio>(
        `/api/financeiro/relatorio?start=${periodo.inicio}T00:00:00Z&end=${periodo.fim}T23:59:59Z`
      )
      .then((r) => {
        if (ativo) setRelatorio(r);
      })
      .catch(() => {
        if (ativo) setRelatorio(null);
      })
      .finally(() => {
        if (ativo) setCarregandoRelatorio(false);
      });
    return () => {
      ativo = false;
    };
  }, [periodo]);

  const nomeCliente = (id: string) => clientes.find((c) => c.id === id)?.name || "—";

  async function criar(e: FormEvent) {
    e.preventDefault();
    if (valorCentavos <= 0) {
      setErroForm("Informe um valor maior que zero.");
      return;
    }
    setErroForm(null);
    setSalvando(true);
    try {
      await api.post("/api/financeiro", {
        clientId,
        caseId: caseId || null,
        description: descricao,
        amount: valorCentavos / 100,
        status,
        dueDate: `${vencimento}T00:00:00Z`,
      });
      setClientId("");
      setCaseId("");
      setDescricao("");
      setValorCentavos(0);
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
      <div className="mb-5.5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Financeiro</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {itens.length} honorário(s) · recebido:{" "}
            <strong className="text-emerald-600 dark:text-emerald-400">{moeda(total)}</strong> · via{" "}
            <code>financeiro-service</code>
          </p>
        </div>
        <Button size="lg" onClick={() => setAberto(true)}>
          + Novo honorário
        </Button>
      </div>

      <Card className="mb-5.5 p-5">
        <div className="flex flex-wrap items-end justify-between gap-5">
          <div>
            <div className="text-[15px] font-semibold">Relatório por período</div>
            <p className="mt-0.5 text-sm text-muted-foreground">
              Honorários com vencimento dentro do intervalo.
            </p>
          </div>
          <div className="flex items-end gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="periodo-inicio" className="text-xs">De</Label>
              <Input
                id="periodo-inicio"
                type="date"
                value={periodo.inicio}
                max={periodo.fim}
                onChange={(e) => setPeriodo((p) => ({ ...p, inicio: e.target.value }))}
                className="w-40"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="periodo-fim" className="text-xs">Até</Label>
              <Input
                id="periodo-fim"
                type="date"
                value={periodo.fim}
                min={periodo.inicio}
                onChange={(e) => setPeriodo((p) => ({ ...p, fim: e.target.value }))}
                className="w-40"
              />
            </div>
          </div>
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-3">
          {[
            { rotulo: "Recebido", valor: relatorio?.totalPago, cor: "text-emerald-700 dark:text-emerald-400" },
            { rotulo: "A receber", valor: relatorio?.totalPendente, cor: "text-amber-700 dark:text-amber-400" },
            { rotulo: "Atrasado", valor: relatorio?.totalAtrasado, cor: "text-red-700 dark:text-red-400" },
          ].map((t) => (
            <div key={t.rotulo} className="rounded-xl border border-border bg-muted/40 px-4 py-3.5">
              <div className="text-xs font-semibold text-muted-foreground">{t.rotulo}</div>
              <div className={cn("mt-1 text-xl font-bold tabular-nums", t.cor)}>
                {carregandoRelatorio ? "—" : moeda(t.valor ?? 0)}
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="gap-0 overflow-hidden py-0">
        {carregando ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Carregando...</div>
        ) : erro ? (
          <div className="p-10 text-center text-sm text-destructive">{erro}</div>
        ) : itens.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Nenhum honorário ainda.</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-muted-foreground">
                <th className="px-5 py-3.5 font-semibold">DESCRIÇÃO</th>
                <th className="px-5 py-3.5 font-semibold">CLIENTE</th>
                <th className="px-5 py-3.5 font-semibold">VALOR</th>
                <th className="px-5 py-3.5 font-semibold">VENCIMENTO</th>
                <th className="px-5 py-3.5 font-semibold">STATUS</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((i) => (
                <tr key={i.id} className="border-t border-border text-sm">
                  <td className="px-5 py-3.5 font-semibold">{i.description}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{nomeCliente(i.clientId)}</td>
                  <td className="px-5 py-3.5 font-semibold">{moeda(i.amount)}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">
                    {new Date(i.dueDate).toLocaleDateString("pt-BR", { timeZone: "UTC" })}
                  </td>
                  <td className="px-5 py-3.5">
                    <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", corStatus[i.status] || corStatus.CANCELADO)}>
                      {i.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Paginacao page={pagina} totalPages={totalPaginas} totalElements={totalRegistros} onChange={setPagina} />

      <Dialog open={aberto} onOpenChange={setAberto}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Novo honorário</DialogTitle>
          </DialogHeader>
          <form onSubmit={criar} className="flex flex-col gap-3.5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="cliente">Cliente *</Label>
              <select id="cliente" value={clientId} onChange={(e) => setClientId(e.target.value)} required className={selectClass}>
                <option value="">Selecione...</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="processo">Processo (opcional)</Label>
              <select id="processo" value={caseId} onChange={(e) => setCaseId(e.target.value)} className={selectClass}>
                <option value="">Nenhum</option>
                {processos.map((p) => (
                  <option key={p.id} value={p.id}>{p.number}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="descricao">Descrição *</Label>
              <Input id="descricao" value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
            </div>
            <div className="flex gap-3">
              <div className="flex flex-1 flex-col gap-1.5">
                <Label htmlFor="valor">Valor *</Label>
                <Input
                  id="valor"
                  type="text"
                  inputMode="numeric"
                  value={valorCentavos ? moeda(valorCentavos / 100) : ""}
                  onChange={(e) => {
                    const digitos = e.target.value.replace(/\D/g, "");
                    setValorCentavos(digitos ? parseInt(digitos, 10) : 0);
                  }}
                  placeholder="R$ 0,00"
                />
              </div>
              <div className="flex flex-1 flex-col gap-1.5">
                <Label htmlFor="vencimento">Vencimento *</Label>
                <Input id="vencimento" type="date" value={vencimento} onChange={(e) => setVencimento(e.target.value)} required />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="status">Status</Label>
              <select id="status" value={status} onChange={(e) => setStatus(e.target.value)} className={selectClass}>
                {STATUS.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
            {erroForm && <div className="text-[13px] text-destructive">{erroForm}</div>}
            <DialogFooter>
              <Button type="button" variant="ghost" size="lg" onClick={() => setAberto(false)}>
                Cancelar
              </Button>
              <Button type="submit" size="lg" disabled={salvando}>
                {salvando ? "Salvando..." : "Salvar"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
