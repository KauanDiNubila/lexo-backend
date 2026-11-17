import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { AuthShell } from "@/components/AuthShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function EsqueciSenha() {
  const [email, setEmail] = useState("");
  const [enviado, setEnviado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      await api.post("/api/auth/senha/esqueci", { email });
      setEnviado(true);
    } catch (err) {
      setErro((err as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <AuthShell
      destaque="Recupere o acesso à sua conta."
      apoio="Informe o email da sua conta e enviaremos um link para definir uma nova senha."
    >
      <h1 className="text-2xl font-semibold tracking-tight">Esqueci minha senha</h1>

      {enviado ? (
        <>
          <p className="mt-1.5 text-sm text-muted-foreground">
            Se existir uma conta com <strong className="text-foreground">{email}</strong>, enviamos um link para
            redefinir a senha. Ele vale por 1 hora e só pode ser usado uma vez.
          </p>
          <Button asChild size="lg" variant="outline" className="mt-8 w-full">
            <Link to="/login">Voltar para o login</Link>
          </Button>
        </>
      ) : (
        <>
          <p className="mt-1.5 text-sm text-muted-foreground">Enviaremos o link para o seu email.</p>

          <form onSubmit={enviar} className="mt-8 flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                autoFocus
                required
              />
            </div>

            {erro && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
                {erro}
              </div>
            )}

            <Button type="submit" size="lg" disabled={enviando} className="mt-1 w-full">
              {enviando ? "Enviando..." : "Enviar link"}
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-muted-foreground">
            Lembrou a senha?{" "}
            <Link to="/login" className="font-semibold text-primary hover:underline">
              Fazer login
            </Link>
          </div>
        </>
      )}
    </AuthShell>
  );
}
