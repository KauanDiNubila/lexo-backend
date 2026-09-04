import { useEffect, useState, type FormEvent } from "react";
import { Sparkles } from "lucide-react";
import { api, type Pagina } from "../lib/api";
import { Paginacao } from "../components/Paginacao";
import { Markdown } from "../components/Markdown";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

type Prazo = { id: string; caseId: string; title: string; status: string; date: string };
type Andamento = { id: string; title: string; description: string | null; date: string };

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
  ATIVO: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
  SUSPENSO: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
  ARQUIVADO: "bg-zinc-500/10 text-zinc-600 dark:text-zinc-400",
  ENCERRADO: "bg-red-500/10 text-red-700 dark:text-red-400",
};

const FORM_VAZIO = { clientId: "", numero: "", area: "", status: "ATIVO" };

const TODOS_STATUS = "__todos__";

function AcaoLinha({ tom = "muted", ...props }: React.ComponentProps<"button"> & { tom?: "muted" | "primary" | "destructive" }) {
  const tons = {
    muted: "text-foreground/80 hover:text-foreground",
    primary: "text-primary hover:underline",
    destructive: "text-destructive hover:underline",
  };
  return <button className={cn("text-[13px] font-medium", tons[tom])} {...props} />;
}

export function Processos() {
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalRegistros, setTotalRegistros] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [busca, setBusca] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");

  const [aberto, setAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  const [iaAberto, setIaAberto] = useState(false);
  const [iaProcesso, setIaProcesso] = useState<Processo | null>(null);
  const [iaCarregando, setIaCarregando] = useState(false);
  const [iaResumo, setIaResumo] = useState("");
  const [iaFonte, setIaFonte] = useState<string>("");

  const [andAberto, setAndAberto] = useState(false);
  const [andProcesso, setAndProcesso] = useState<Processo | null>(null);
  const [andamentos, setAndamentos] = useState<Andamento[]>([]);
  const [andCarregando, setAndCarregando] = useState(false);
  const [andForm, setAndForm] = useState({ title: "", description: "", date: "" });
  const [andSalvando, setAndSalvando] = useState(false);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [ps, cs] = await Promise.all([
        api.get<Pagina<Processo>>(
          `/api/processos?page=${pagina}&q=${encodeURIComponent(busca.trim())}` +
            (filtroStatus ? `&status=${filtroStatus}` : "")
        ),
        api.get<Cliente[]>("/api/clientes/opcoes"),
      ]);
      setProcessos(ps.content);
      setTotalPaginas(ps.totalPages);
      setTotalRegistros(ps.totalElements);
      setClientes(cs);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    const timer = setTimeout(carregar, busca ? 300 : 0);
    return () => clearTimeout(timer);
  }, [pagina, busca, filtroStatus]);

  useEffect(() => {
    setPagina(0);
  }, [busca, filtroStatus]);

  const nomeCliente = (id: string) => clientes.find((c) => c.id === id)?.name || "—";

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
    if (!form.clientId) {
      setErroForm("Selecione um cliente.");
      return;
    }
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

  async function abrirAndamentos(p: Processo) {
    setAndProcesso(p);
    setAndAberto(true);
    setAndForm({ title: "", description: "", date: "" });
    setAndCarregando(true);
    try {
      setAndamentos(await api.get<Andamento[]>(`/api/processos/${p.id}/andamentos`));
    } catch {
      setAndamentos([]);
    } finally {
      setAndCarregando(false);
    }
  }

  async function adicionarAndamento(e: FormEvent) {
    e.preventDefault();
    if (!andProcesso || !andForm.title.trim() || !andForm.date || andSalvando) return;
    setAndSalvando(true);
    try {
      await api.post(`/api/processos/${andProcesso.id}/andamentos`, {
        title: andForm.title.trim(),
        description: andForm.description.trim() || null,
        date: `${andForm.date}T00:00:00Z`,
      });
      setAndForm({ title: "", description: "", date: "" });
      setAndamentos(await api.get<Andamento[]>(`/api/processos/${andProcesso.id}/andamentos`));
    } finally {
      setAndSalvando(false);
    }
  }

  async function excluirAndamento(id: string) {
    if (!andProcesso) return;
    await api.del(`/api/processos/${andProcesso.id}/andamentos/${id}`);
    setAndamentos(await api.get<Andamento[]>(`/api/processos/${andProcesso.id}/andamentos`));
  }

  async function resumirIA(p: Processo) {
    setIaProcesso(p);
    setIaAberto(true);
    setIaCarregando(true);
    setIaResumo("");
    setIaFonte("");
    try {
      const agenda = await api
        .get<Pagina<Prazo>>(`/api/agenda?caseId=${p.id}&size=100`)
        .then((pg) => pg.content)
        .catch(() => [] as Prazo[]);
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
    } catch {
      setIaResumo("Não foi possível gerar o resumo agora. Tente novamente.");
      setIaFonte("erro");
    } finally {
      setIaCarregando(false);
    }
  }

  return (
    <div>
      <div className="mb-5.5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Processos</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {busca || filtroStatus ? `${totalRegistros} encontrado(s)` : `${totalRegistros} processo(s)`} · via{" "}
            <code>processo-service</code>
          </p>
        </div>
        <Button size="lg" onClick={abrirNovo}>
          + Novo processo
        </Button>
      </div>

      <div className="mb-3.5 flex flex-wrap gap-2.5">
        <Input
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar por número, cliente ou área..."
          className="max-w-[360px]"
        />
        <Select
          value={filtroStatus || TODOS_STATUS}
          onValueChange={(v) => setFiltroStatus(v === TODOS_STATUS ? "" : v)}
        >
          <SelectTrigger className="max-w-[180px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={TODOS_STATUS}>Todos os status</SelectItem>
            {STATUS.map((s) => (
              <SelectItem key={s} value={s}>{s}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Card className="gap-0 overflow-hidden py-0">
        {carregando ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Carregando...</div>
        ) : erro ? (
          <div className="p-10 text-center text-sm text-destructive">{erro}</div>
        ) : processos.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">
            {busca || filtroStatus ? "Nenhum processo corresponde ao filtro." : "Nenhum processo ainda."}
          </div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-muted-foreground">
                <th className="px-5 py-3.5 font-semibold">NÚMERO</th>
                <th className="px-5 py-3.5 font-semibold">CLIENTE</th>
                <th className="px-5 py-3.5 font-semibold">ÁREA</th>
                <th className="px-5 py-3.5 font-semibold">STATUS</th>
                <th className="px-5 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {processos.map((p) => (
                <tr key={p.id} className="border-t border-border text-sm">
                  <td className="px-5 py-3.5 font-semibold">{p.number}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{nomeCliente(p.clientId)}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{p.area || "—"}</td>
                  <td className="px-5 py-3.5">
                    <span className={cn("rounded-full px-2.5 py-1 text-xs font-semibold", corStatus[p.status] || corStatus.ARQUIVADO)}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-5 py-3.5 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-3.5">
                      <AcaoLinha onClick={() => abrirAndamentos(p)} title="Ver/adicionar andamentos">
                        Andamentos
                      </AcaoLinha>
                      <AcaoLinha tom="primary" onClick={() => resumirIA(p)} title="Resumir com a Lexo IA">
                        <span className="inline-flex items-center gap-1">
                          <Sparkles size={13} /> Resumir
                        </span>
                      </AcaoLinha>
                      <AcaoLinha tom="primary" onClick={() => abrirEdicao(p)}>
                        Editar
                      </AcaoLinha>
                      <AcaoLinha tom="destructive" onClick={() => excluir(p.id)}>
                        Excluir
                      </AcaoLinha>
                    </div>
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
            <DialogTitle>{editandoId ? "Editar processo" : "Novo processo"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={salvar} className="flex flex-col gap-3.5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="cliente">Cliente *</Label>
              <Select
                value={form.clientId}
                onValueChange={(v) => setForm({ ...form, clientId: v })}
              >
                <SelectTrigger id="cliente">
                  <SelectValue placeholder="Selecione..." />
                </SelectTrigger>
                <SelectContent>
                  {clientes.map((c) => (
                    <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="numero">Número do processo *</Label>
              <Input id="numero" value={form.numero} onChange={(e) => setForm({ ...form, numero: e.target.value })} required placeholder="0001-23.2026" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="area">Área</Label>
              <Input id="area" value={form.area} onChange={(e) => setForm({ ...form, area: e.target.value })} placeholder="Cível" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="status">Status</Label>
              <Select
                value={form.status}
                onValueChange={(v) => setForm({ ...form, status: v })}
              >
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STATUS.map((s) => (
                    <SelectItem key={s} value={s}>{s}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {erroForm && <div className="text-[13px] text-destructive">{erroForm}</div>}
            <DialogFooter>
              <Button type="button" variant="ghost" size="lg" onClick={() => setAberto(false)}>
                Cancelar
              </Button>
              <Button type="submit" size="lg" disabled={salvando}>
                {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Salvar"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={iaAberto} onOpenChange={setIaAberto}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2.5">
              <span className="grid size-7 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
                <Sparkles size={15} />
              </span>
              Resumo do processo
            </DialogTitle>
            <p className="mt-1 text-[13px] text-muted-foreground">{iaProcesso?.number}</p>
          </DialogHeader>

          {iaCarregando ? (
            <div className="py-5 text-sm text-muted-foreground">Analisando o processo...</div>
          ) : (
            <>
              <div className="text-[14.5px]">
                <Markdown>{iaResumo}</Markdown>
              </div>
              {iaFonte && iaFonte !== "erro" && (
                <div className="mt-3.5 text-xs text-muted-foreground">
                  {iaFonte === "gemini"
                    ? "✨ Gerado pela Lexo IA (Gemini)"
                    : "Resumo automático · configure a Lexo IA (GEMINI_API_KEY) para análises mais ricas"}
                </div>
              )}
            </>
          )}

          <DialogFooter>
            <Button variant="ghost" size="lg" onClick={() => setIaAberto(false)}>
              Fechar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={andAberto} onOpenChange={setAndAberto}>
        <DialogContent className="max-h-[88vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Andamentos</DialogTitle>
            <p className="mt-0.5 text-[13px] text-muted-foreground">
              {andProcesso?.number} — o cliente também vê estes andamentos no portal.
            </p>
          </DialogHeader>

          {andCarregando ? (
            <div className="text-sm text-muted-foreground">Carregando...</div>
          ) : andamentos.length === 0 ? (
            <div className="mb-2 text-sm text-muted-foreground">Nenhum andamento ainda.</div>
          ) : (
            <div className="mb-2 flex flex-col">
              {andamentos.map((a) => (
                <div key={a.id} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <span className="mt-1.5 size-2.5 shrink-0 rounded-full bg-primary" />
                    <span className="w-px flex-1 bg-border" />
                  </div>
                  <div className="flex-1 pb-4">
                    <div className="flex justify-between gap-2">
                      <span className="text-sm font-semibold">{a.title}</span>
                      <button onClick={() => excluirAndamento(a.id)} className="shrink-0 text-xs text-destructive hover:underline">
                        remover
                      </button>
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {new Date(a.date).toLocaleDateString("pt-BR", { timeZone: "UTC" })}
                    </div>
                    {a.description && <div className="mt-0.5 text-[13px]">{a.description}</div>}
                  </div>
                </div>
              ))}
            </div>
          )}

          <form onSubmit={adicionarAndamento} className="mt-3 flex flex-col gap-2.5 border-t border-border pt-4">
            <div className="text-[13px] font-semibold text-muted-foreground">Novo andamento</div>
            <div className="flex gap-2.5">
              <Input
                placeholder="Ex.: Sentença publicada"
                value={andForm.title}
                onChange={(e) => setAndForm({ ...andForm, title: e.target.value })}
                required
              />
              <Input
                type="date"
                className="max-w-[160px]"
                value={andForm.date}
                onChange={(e) => setAndForm({ ...andForm, date: e.target.value })}
                required
              />
            </div>
            <Textarea
              placeholder="Detalhes (opcional)"
              rows={2}
              value={andForm.description}
              onChange={(e) => setAndForm({ ...andForm, description: e.target.value })}
            />
            <div className="flex justify-end gap-2.5">
              <Button type="button" variant="ghost" onClick={() => setAndAberto(false)}>
                Fechar
              </Button>
              <Button type="submit" disabled={andSalvando || !andForm.title.trim() || !andForm.date}>
                {andSalvando ? "Adicionando..." : "Adicionar"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
