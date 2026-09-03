import { useEffect, useRef, useState, type FormEvent } from "react";
import { Sparkles, Send, Copy, Check, MessageSquare, FileText } from "lucide-react";
import { api } from "../lib/api";
import { Markdown } from "../components/Markdown";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

type Msg = { papel: "user" | "assistant"; texto: string };
type Processo = { id: string; number: string; area: string | null; status: string; clientId: string };
type Cliente = { id: string; name: string };

const SUGESTOES = [
  "Como funciona a contestação no processo civil?",
  "Explique, em linguagem simples, o que é jurimetria.",
  "Quais os prazos prescricionais trabalhistas mais comuns?",
];

const EXEMPLOS = [
  "Petição inicial de cobrança de honorários advocatícios",
  "Notificação extrajudicial de rescisão contratual",
  "Contestação em ação de cobrança",
];

const ABAS = [
  { id: "chat" as const, rotulo: "Conversar", icone: MessageSquare },
  { id: "peticao" as const, rotulo: "Rascunhar petição", icone: FileText },
];

function AbaChat() {
  const [mensagens, setMensagens] = useState<Msg[]>([]);
  const [input, setInput] = useState("");
  const [enviando, setEnviando] = useState(false);
  const fimRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fimRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [mensagens, enviando]);

  async function perguntar(texto: string) {
    if (!texto.trim() || enviando) return;
    const novas: Msg[] = [...mensagens, { papel: "user", texto: texto.trim() }];
    setMensagens(novas);
    setInput("");
    setEnviando(true);
    try {
      const r = await api.post<{ resposta: string; fonte: string }>("/api/ia/chat", { mensagens: novas });
      setMensagens([...novas, { papel: "assistant", texto: r.resposta }]);
    } catch {
      setMensagens([...novas, { papel: "assistant", texto: "Não consegui responder agora. Tente novamente em instantes." }]);
    } finally {
      setEnviando(false);
    }
  }

  function enviar(e: FormEvent) {
    e.preventDefault();
    perguntar(input);
  }

  return (
    <div className="flex h-[calc(100vh-260px)] min-h-[420px] flex-col">
      <Card className="flex-1 gap-3.5 overflow-y-auto p-5">
        {mensagens.length === 0 ? (
          <div className="m-auto max-w-md text-center">
            <div className="mx-auto mb-3.5 grid size-13 place-items-center rounded-2xl bg-primary/10 text-primary">
              <Sparkles size={26} />
            </div>
            <div className="text-base font-bold">Como posso ajudar?</div>
            <p className="mt-1.5 mb-4.5 text-sm text-muted-foreground">
              Pergunte sobre prazos, procedimentos, ou peça para explicar um termo jurídico.
            </p>
            <div className="flex flex-col gap-2">
              {SUGESTOES.map((s) => (
                <button
                  key={s}
                  onClick={() => perguntar(s)}
                  className="rounded-xl border border-border bg-muted px-3.5 py-2.5 text-left text-[13.5px] hover:bg-muted/70"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        ) : (
          mensagens.map((m, i) => (
            <div key={i} className={cn("flex", m.papel === "user" ? "justify-end" : "justify-start")}>
              <div
                className={cn(
                  "max-w-[78%] rounded-2xl px-4 py-2.5 text-[14.5px] leading-relaxed",
                  m.papel === "user"
                    ? "rounded-tr-sm bg-primary text-primary-foreground"
                    : "rounded-tl-sm bg-muted text-foreground"
                )}
              >
                {m.papel === "assistant" ? <Markdown>{m.texto}</Markdown> : <span className="whitespace-pre-wrap">{m.texto}</span>}
              </div>
            </div>
          ))
        )}
        {enviando && (
          <div className="flex justify-start">
            <div className="rounded-2xl bg-muted px-4 py-2.5 text-sm text-muted-foreground">Lexo IA está pensando...</div>
          </div>
        )}
        <div ref={fimRef} />
      </Card>

      <form onSubmit={enviar} className="mt-3.5 flex gap-2.5">
        <Input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Escreva sua pergunta..." disabled={enviando} />
        <Button type="submit" size="icon-lg" disabled={enviando || !input.trim()}>
          <Send size={16} />
        </Button>
      </form>
      <p className="mt-2 text-center text-[11px] text-muted-foreground">
        A Lexo IA pode cometer erros. Confira informações importantes.
      </p>
    </div>
  );
}

function AbaPeticao() {
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
      api.get<Processo[]>("/api/processos/opcoes").catch(() => [] as Processo[]),
      api.get<Cliente[]>("/api/clientes/opcoes").catch(() => [] as Cliente[]),
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
    <div className="mx-auto max-w-3xl">
      <Card className="p-6">
        <form onSubmit={gerar} className="flex flex-col">
          <Label htmlFor="pedido">O que você quer gerar? *</Label>
          <Textarea
            id="pedido"
            className="mt-1.5"
            value={pedido}
            onChange={(e) => setPedido(e.target.value)}
            placeholder="Ex.: petição inicial de cobrança de honorários não pagos"
            rows={3}
            required
          />
          <div className="mt-2.5 mb-1 flex flex-wrap gap-2">
            {EXEMPLOS.map((ex) => (
              <button
                key={ex}
                type="button"
                onClick={() => setPedido(ex)}
                className="rounded-full border border-border bg-muted px-2.5 py-1.5 text-xs text-muted-foreground hover:bg-muted/70"
              >
                {ex}
              </button>
            ))}
          </div>

          <div className="mt-3.5 flex flex-col gap-1.5">
            <Label htmlFor="processo">Vincular a um processo (opcional — dá contexto à IA)</Label>
            <select
              id="processo"
              value={processoId}
              onChange={(e) => setProcessoId(e.target.value)}
              className="flex h-9 w-full max-w-sm rounded-lg border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <option value="">Nenhum</option>
              {processos.map((p) => (
                <option key={p.id} value={p.id}>{p.number} — {nomeCliente(p.clientId)}</option>
              ))}
            </select>
          </div>

          <Button type="submit" size="lg" className="mt-5 w-fit" disabled={gerando || !pedido.trim()}>
            <Sparkles size={15} /> {gerando ? "Redigindo minuta..." : "Gerar minuta"}
          </Button>
        </form>
      </Card>

      {(texto || gerando) && (
        <Card className="mt-4 p-6">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-[15px] font-bold">Minuta gerada</h2>
            {texto && (
              <Button variant="ghost" size="sm" onClick={copiar}>
                {copiado ? <><Check size={14} /> Copiado</> : <><Copy size={14} /> Copiar</>}
              </Button>
            )}
          </div>
          {gerando ? (
            <div className="py-3 text-sm text-muted-foreground">Redigindo a minuta...</div>
          ) : (
            <>
              <div className="max-h-[520px] overflow-y-auto rounded-[10px] border border-border bg-background px-5 py-4.5 text-sm">
                <Markdown>{texto}</Markdown>
              </div>
              <p className="mt-2.5 text-xs text-muted-foreground">
                {fonte === "gemini" ? "✨ Gerado pela Lexo IA (Gemini)" : "Modelo automático · configure a Lexo IA para geração completa"}
                {" · "}Minuta a revisar pelo advogado responsável.
              </p>
            </>
          )}
        </Card>
      )}
    </div>
  );
}

export function LexoIA() {
  const [aba, setAba] = useState<(typeof ABAS)[number]["id"]>("chat");

  return (
    <div>
      <div className="mb-5">
        <h1 className="flex items-center gap-2.5 text-2xl font-bold tracking-tight">
          <span className="grid size-8 place-items-center rounded-lg bg-primary/10 text-primary">
            <Sparkles size={17} />
          </span>
          Lexo IA
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Converse, resuma processos e rascunhe petições · via <code>ia-service</code>
        </p>
      </div>

      <div className="mb-5 flex gap-1.5 border-b border-border">
        {ABAS.map((a) => (
          <button
            key={a.id}
            onClick={() => setAba(a.id)}
            className={cn(
              "flex items-center gap-1.5 border-b-2 px-3 pb-2.5 text-sm font-semibold transition-colors",
              aba === a.id
                ? "border-primary text-foreground"
                : "border-transparent text-muted-foreground hover:text-foreground"
            )}
          >
            <a.icone size={15} />
            {a.rotulo}
          </button>
        ))}
      </div>

      {aba === "chat" ? <AbaChat /> : <AbaPeticao />}
    </div>
  );
}
