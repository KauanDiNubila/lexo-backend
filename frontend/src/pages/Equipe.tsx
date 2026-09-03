import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
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

type Usuario = { id: string; name: string; email: string; role: string; totpEnabled: boolean };
type Convite = { id: string; name: string; email: string; role: string };

const PAPEIS = ["ADMIN", "ADVOGADO", "SECRETARIA"];

const selectClass =
  "flex h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

export function Equipe() {
  const { usuario: eu } = useAuth();
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [convites, setConvites] = useState<Convite[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [aberto, setAberto] = useState(false);
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [papel, setPapel] = useState("ADVOGADO");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      const [us, cs] = await Promise.all([
        api.get<Usuario[]>("/api/usuarios"),
        api.get<Convite[]>("/api/usuarios/convites"),
      ]);
      setUsuarios(us);
      setConvites(cs);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function convidar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    try {
      await api.post("/api/usuarios/convites", { name: nome, email, role: papel });
      setNome("");
      setEmail("");
      setPapel("ADVOGADO");
      setAberto(false);
      await carregar();
    } catch (e) {
      setErroForm((e as Error).message);
    } finally {
      setSalvando(false);
    }
  }

  async function mudarPapel(userId: string, role: string) {
    try {
      await api.patch("/api/usuarios/papel", { userId, role });
      await carregar();
    } catch (e) {
      alert((e as Error).message);
    }
  }

  async function remover(id: string) {
    if (!confirm("Remover este usuário?")) return;
    await api.del(`/api/usuarios/${id}`);
    await carregar();
  }

  async function revogar(id: string) {
    await api.del(`/api/usuarios/convites/${id}`);
    await carregar();
  }

  return (
    <div>
      <div className="mb-5.5 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Equipe</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {usuarios.length} usuário(s) · via <code>auth-service</code>
          </p>
        </div>
        <Button size="lg" onClick={() => setAberto(true)}>
          + Convidar
        </Button>
      </div>

      {erro && <div className="mb-4 text-sm text-destructive">{erro}</div>}

      <Card className="mb-5.5 gap-0 overflow-hidden py-0">
        {carregando ? (
          <div className="p-10 text-center text-sm text-muted-foreground">Carregando...</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="text-left text-xs text-muted-foreground">
                <th className="px-5 py-3.5 font-semibold">NOME</th>
                <th className="px-5 py-3.5 font-semibold">EMAIL</th>
                <th className="px-5 py-3.5 font-semibold">PAPEL</th>
                <th className="px-5 py-3.5 font-semibold">2FA</th>
                <th className="px-5 py-3.5" />
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} className="border-t border-border text-sm">
                  <td className="px-5 py-3.5 font-semibold">
                    {u.name} {u.id === eu?.id && <span className="text-xs text-primary">(você)</span>}
                  </td>
                  <td className="px-5 py-3.5 text-muted-foreground">{u.email}</td>
                  <td className="px-5 py-3.5">
                    {u.id === eu?.id ? (
                      <span className="text-muted-foreground">{u.role}</span>
                    ) : (
                      <select
                        value={u.role}
                        onChange={(e) => mudarPapel(u.id, e.target.value)}
                        className="h-8 rounded-md border border-input bg-transparent px-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                      >
                        {PAPEIS.map((p) => (
                          <option key={p} value={p}>{p}</option>
                        ))}
                      </select>
                    )}
                  </td>
                  <td className="px-5 py-3.5">
                    {u.totpEnabled ? (
                      <span className="text-emerald-600 dark:text-emerald-400">●</span>
                    ) : (
                      <span className="text-muted-foreground">○</span>
                    )}
                  </td>
                  <td className="px-5 py-3.5 text-right">
                    {u.id !== eu?.id && (
                      <button onClick={() => remover(u.id)} className="text-[13px] font-medium text-destructive hover:underline">
                        Remover
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      {convites.length > 0 && (
        <>
          <h2 className="mb-2.5 text-[15px] font-semibold text-muted-foreground">Convites pendentes</h2>
          <Card className="gap-0 overflow-hidden py-0">
            <table className="w-full border-collapse">
              <tbody>
                {convites.map((c) => (
                  <tr key={c.id} className="border-t border-border text-sm first:border-t-0">
                    <td className="px-5 py-3 font-semibold">{c.name}</td>
                    <td className="px-5 py-3 text-muted-foreground">{c.email}</td>
                    <td className="px-5 py-3 text-muted-foreground">{c.role}</td>
                    <td className="px-5 py-3 text-right">
                      <button onClick={() => revogar(c.id)} className="text-[13px] font-medium text-destructive hover:underline">
                        Revogar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </>
      )}

      <Dialog open={aberto} onOpenChange={setAberto}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Convidar para a equipe</DialogTitle>
          </DialogHeader>
          <form onSubmit={convidar} className="flex flex-col gap-3.5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="nome">Nome *</Label>
              <Input id="nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email *</Label>
              <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="papel">Papel</Label>
              <select id="papel" value={papel} onChange={(e) => setPapel(e.target.value)} className={selectClass}>
                {PAPEIS.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
            {erroForm && <div className="text-[13px] text-destructive">{erroForm}</div>}
            <DialogFooter>
              <Button type="button" variant="ghost" size="lg" onClick={() => setAberto(false)}>
                Cancelar
              </Button>
              <Button type="submit" size="lg" disabled={salvando}>
                {salvando ? "Enviando..." : "Enviar convite"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
