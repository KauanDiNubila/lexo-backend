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
  CRITICO: "text-red-600 dark:text-red-400",
  URGENTE: "text-orange-600 dark:text-orange-400",
  ALTO: "text-amber-600 dark:text-amber-400",
  MEDIO: "text-blue-600 dark:text-blue-400",
  BAIXO: "text-emerald-600 dark:text-emerald-400",
};

const selectClass =
  "flex h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

export function Agenda() {
  const [prazos, setPrazos] = useState<Prazo[]>([]);
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalRegistros, setTotalRegistros] = useState(0);
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
      const [pg, procs] = await Promise.all([
        api.get<Pagina<Prazo>>(`/api/agenda?page=${pagina}`),
        api.get<Processo[]>("/api/processos/opcoes"),
      ]);
      setPrazos(pg.content);
      setTotalPaginas(pg.totalPages);
      setTotalRegistros(pg.totalElements);
      setProcessos(procs);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, [pagina]);

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
      <div className="mb-5.5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Agenda</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {prazos.length} compromisso(s) · via <code>processo-service</code>
          </p>
        </div>
        <Button size="lg" onClick={() => setAberto(true)}>
          + Novo prazo
        </Button>
      </div>

      <Card className="gap-0 overflow-hidden py-0">
        {carregando ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Carregando...</div>
        ) : erro ? (
          <div className="p-10 text-center text-sm text-destructive">{erro}</div>
        ) : prazos.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Nenhum prazo ainda.</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-muted-foreground">
                <th className="px-5 py-3.5 font-semibold">TÍTULO</th>
                <th className="px-5 py-3.5 font-semibold">PROCESSO</th>
                <th className="px-5 py-3.5 font-semibold">TIPO</th>
                <th className="px-5 py-3.5 font-semibold">DATA</th>
                <th className="px-5 py-3.5 font-semibold">RISCO</th>
                <th className="px-5 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {prazos.map((p) => (
                <tr key={p.id} className="border-t border-border text-sm">
                  <td
                    className={cn(
                      "px-5 py-3.5 font-semibold",
                      p.status === "CONCLUIDO" && "text-muted-foreground line-through"
                    )}
                  >
                    {p.title}
                  </td>
                  <td className="px-5 py-3.5 text-muted-foreground">{numProcesso(p.caseId)}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{p.type}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">
                    {new Date(p.date).toLocaleDateString("pt-BR", { timeZone: "UTC" })}
                  </td>
                  <td className="px-5 py-3.5">
                    {p.risk ? (
                      <span className={cn("text-xs font-bold", corRisco[p.risk] || "text-muted-foreground")}>{p.risk}</span>
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </td>
                  <td className="px-5 py-3.5 text-right">
                    <button
                      onClick={() => alternar(p.id, p.status !== "CONCLUIDO")}
                      className="text-[13px] font-medium text-primary hover:underline"
                    >
                      {p.status === "CONCLUIDO" ? "Reabrir" : "Concluir"}
                    </button>
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
            <DialogTitle>Novo prazo</DialogTitle>
          </DialogHeader>
          <form onSubmit={criar} className="flex flex-col gap-3.5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="processo">Processo *</Label>
              <select id="processo" value={caseId} onChange={(e) => setCaseId(e.target.value)} required className={selectClass}>
                <option value="">Selecione...</option>
                {processos.map((p) => (
                  <option key={p.id} value={p.id}>{p.number}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="titulo">Título *</Label>
              <Input id="titulo" value={titulo} onChange={(e) => setTitulo(e.target.value)} required placeholder="Contestação" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="tipo">Tipo</Label>
              <select id="tipo" value={tipo} onChange={(e) => setTipo(e.target.value)} className={selectClass}>
                {TIPOS.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="data">Data *</Label>
              <Input id="data" type="date" value={data} onChange={(e) => setData(e.target.value)} required />
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
