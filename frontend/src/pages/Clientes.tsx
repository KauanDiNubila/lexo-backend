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

type Cliente = {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  portalAtivo: boolean;
  portalExpiraEm: string | null;
  createdAt: string | null;
};

const FORM_VAZIO = { nome: "", documento: "", email: "", telefone: "" };

function AcaoLinha({ tom = "muted", ...props }: React.ComponentProps<"button"> & { tom?: "muted" | "primary" | "success" | "destructive" }) {
  const tons = {
    muted: "text-muted-foreground hover:text-foreground",
    primary: "text-primary hover:underline",
    success: "text-emerald-600 hover:underline dark:text-emerald-400",
    destructive: "text-destructive hover:underline",
  };
  return <button className={cn("text-[13px] font-medium", tons[tom])} {...props} />;
}

export function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [busca, setBusca] = useState("");
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalRegistros, setTotalRegistros] = useState(0);

  const [aberto, setAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const pg = await api.get<Pagina<Cliente>>(
        `/api/clientes?page=${pagina}&q=${encodeURIComponent(busca.trim())}`
      );
      setClientes(pg.content);
      setTotalPaginas(pg.totalPages);
      setTotalRegistros(pg.totalElements);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    const timer = setTimeout(carregar, busca ? 300 : 0);
    return () => clearTimeout(timer);
  }, [pagina, busca]);

  useEffect(() => {
    setPagina(0);
  }, [busca]);

  function abrirNovo() {
    setEditandoId(null);
    setForm(FORM_VAZIO);
    setErroForm(null);
    setAberto(true);
  }

  function abrirEdicao(c: Cliente) {
    setEditandoId(c.id);
    setForm({
      nome: c.name,
      documento: c.document ?? "",
      email: c.email ?? "",
      telefone: c.phone ?? "",
    });
    setErroForm(null);
    setAberto(true);
  }

  async function salvar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    const corpo = {
      name: form.nome,
      document: form.documento || null,
      email: form.email || null,
      phone: form.telefone || null,
    };
    try {
      if (editandoId) {
        await api.put(`/api/clientes/${editandoId}`, corpo);
      } else {
        await api.post("/api/clientes", corpo);
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
    if (!confirm("Excluir este cliente?")) return;
    await api.del(`/api/clientes/${id}`);
    await carregar();
  }

  async function gerarPortal(c: Cliente) {
    try {
      const r = await api.post<{ token: string }>(`/api/clientes/${c.id}/portal`);
      const link = `${window.location.origin}/portal/${r.token}`;
      await navigator.clipboard.writeText(link);
      await carregar();
      alert(
        `Link do portal copiado!

${link}

Envie para ${c.name} acompanhar o processo.
` +
          `O link vale por 90 dias e substitui qualquer link anterior deste cliente.`
      );
    } catch {
      alert("Não foi possível gerar o link. Tente novamente.");
    }
  }

  async function revogarPortal(c: Cliente) {
    if (!confirm(`Revogar o link do portal de ${c.name}? Quem tiver o link perde o acesso.`)) return;
    await api.del(`/api/clientes/${c.id}/portal`);
    await carregar();
  }

  return (
    <div>
      <div className="mb-5.5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Clientes</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {busca ? `${totalRegistros} encontrado(s)` : `${totalRegistros} cadastrado(s)`} · via{" "}
            <code>cliente-service</code>
          </p>
        </div>
        <Button size="lg" onClick={abrirNovo}>
          + Novo cliente
        </Button>
      </div>

      <div className="mb-3.5">
        <Input
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar por nome, documento, email ou telefone..."
          className="max-w-[420px]"
        />
      </div>

      <Card className="gap-0 overflow-hidden py-0">
        {carregando ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Carregando...</div>
        ) : erro ? (
          <div className="p-10 text-center text-sm text-destructive">{erro}</div>
        ) : clientes.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">
            {busca ? "Nenhum cliente corresponde à busca." : "Nenhum cliente ainda. Clique em “Novo cliente”."}
          </div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-muted-foreground">
                <th className="px-5 py-3.5 font-semibold">NOME</th>
                <th className="px-5 py-3.5 font-semibold">DOCUMENTO</th>
                <th className="px-5 py-3.5 font-semibold">EMAIL</th>
                <th className="px-5 py-3.5 font-semibold">TELEFONE</th>
                <th className="px-5 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id} className="border-t border-border text-sm">
                  <td className="px-5 py-3.5 font-semibold">{c.name}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{c.document || "—"}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{c.email || "—"}</td>
                  <td className="px-5 py-3.5 text-muted-foreground">{c.phone || "—"}</td>
                  <td className="px-5 py-3.5 text-right whitespace-nowrap">
                    <div className="flex items-center justify-end gap-3.5">
                      <AcaoLinha
                        tom={c.portalAtivo ? "success" : "muted"}
                        onClick={() => gerarPortal(c)}
                        title={c.portalAtivo ? "Gerar um link novo (invalida o atual)" : "Gerar o link do portal do cliente"}
                      >
                        {c.portalAtivo ? "Novo link" : "Gerar portal"}
                      </AcaoLinha>
                      {c.portalAtivo && (
                        <AcaoLinha onClick={() => revogarPortal(c)} title="Revogar o link do portal">
                          Revogar
                        </AcaoLinha>
                      )}
                      <AcaoLinha tom="primary" onClick={() => abrirEdicao(c)}>
                        Editar
                      </AcaoLinha>
                      <AcaoLinha tom="destructive" onClick={() => excluir(c.id)}>
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
            <DialogTitle>{editandoId ? "Editar cliente" : "Novo cliente"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={salvar} className="flex flex-col gap-3.5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="nome">Nome *</Label>
              <Input id="nome" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="documento">CPF / CNPJ</Label>
              <Input
                id="documento"
                value={form.documento}
                onChange={(e) => setForm({ ...form, documento: e.target.value })}
                placeholder="52998224725"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="telefone">Telefone</Label>
              <Input id="telefone" value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
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
    </div>
  );
}
