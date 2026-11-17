import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { AuthShell } from "@/components/AuthShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function RedefinirSenha() {
  const { token = "" } = useParams();
  const navigate = useNavigate();

  const [senha, setSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [falhouNoServidor, setFalhouNoServidor] = useState(false);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setFalhouNoServidor(false);
    if (senha !== confirmacao) {
      setErro("As senhas não coincidem");
      return;
    }
    setEnviando(true);
    try {
      await api.post("/api/auth/senha/redefinir", { token, password: senha, confirmPassword: confirmacao });
      navigate("/login?senha=redefinida");
    } catch (err) {
      setErro((err as Error).message);
      setFalhouNoServidor(true);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <AuthShell
      destaque="Defina uma nova senha."
      apoio="Ao concluir, todas as sessões abertas da sua conta serão encerradas."
    >
      <h1 className="text-2xl font-semibold tracking-tight">Nova senha</h1>
      <p className="mt-1.5 text-sm text-muted-foreground">Escolha uma senha com ao menos 8 caracteres.</p>

      <form onSubmit={enviar} className="mt-8 flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="senha">Nova senha</Label>
          <Input
            id="senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            minLength={8}
            autoComplete="new-password"
            autoFocus
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="confirmacao">Confirmar senha</Label>
          <Input
            id="confirmacao"
            type="password"
            value={confirmacao}
            onChange={(e) => setConfirmacao(e.target.value)}
            minLength={8}
            autoComplete="new-password"
            required
          />
        </div>

        {erro && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
            {erro}{" "}
            {falhouNoServidor && (
              <Link to="/esqueci-senha" className="font-semibold underline">
                Pedir um novo link
              </Link>
            )}
          </div>
        )}

        <Button type="submit" size="lg" disabled={enviando} className="mt-1 w-full">
          {enviando ? "Salvando..." : "Redefinir senha"}
        </Button>
      </form>

      <div className="mt-6 text-center text-sm text-muted-foreground">
        <Link to="/login" className="font-semibold text-primary hover:underline">
          Voltar para o login
        </Link>
      </div>
    </AuthShell>
  );
}
