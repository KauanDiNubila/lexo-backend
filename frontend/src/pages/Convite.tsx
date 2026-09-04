import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import Silk from "@/components/ui/silk";

type InfoConvite = {
  organizationName: string | null;
  inviteeName: string;
  email: string;
  valid: boolean;
  reason: string | null;
};

export function Convite() {
  const { token = "" } = useParams();
  const navigate = useNavigate();

  const [info, setInfo] = useState<InfoConvite | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erroCarga, setErroCarga] = useState<string | null>(null);

  const [senha, setSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let ativo = true;
    api
      .get<InfoConvite>(`/api/convites/info/${encodeURIComponent(token)}`)
      .then((r) => {
        if (ativo) setInfo(r);
      })
      .catch((e) => {
        if (ativo) setErroCarga((e as Error).message);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [token]);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (senha !== confirmacao) {
      setErro("As senhas não coincidem");
      return;
    }
    setEnviando(true);
    try {
      await api.post("/api/convites/aceitar", { token, password: senha, confirmPassword: confirmacao });
      navigate("/login?convite=aceito");
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setEnviando(false);
    }
  }

  const indisponivel = !carregando && (erroCarga !== null || info?.valid === false);
  const motivo = erroCarga ?? info?.reason ?? "Este convite não é mais válido.";

  return (
    <div className="grid min-h-screen md:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-[#08293d] md:flex">
        <div aria-hidden className="pointer-events-none absolute inset-0 h-full w-full animate-in fade-in duration-500">
          <Silk color="#0b3450" speed={5} scale={1.3} noiseIntensity={1.1} rotation={0.15} />
        </div>
        <div className="relative flex h-full w-full flex-col p-12">
          <Link to="/" className="w-fit text-lg font-bold tracking-tight text-white">
            Lexo
          </Link>
          <div className="flex flex-1 flex-col justify-center">
            <p className="max-w-sm text-3xl leading-snug font-medium text-white">
              Você foi convidado para um escritório no Lexo.
            </p>
            <p className="mt-4 max-w-sm text-sm text-white/60">
              Defina sua senha para ativar o acesso aos processos, prazos e honorários da equipe.
            </p>
          </div>
          <p className="text-xs text-white/40">© {new Date().getFullYear()} Lexo</p>
        </div>
      </div>

      <div className="flex flex-col justify-center px-6 py-16 md:px-16">
        <div className="mx-auto w-full max-w-sm">
          <Link to="/" className="mb-10 inline-flex text-lg font-bold tracking-tight md:hidden">
            Lexo
          </Link>

          {carregando ? (
            <p className="text-sm text-muted-foreground">Verificando convite...</p>
          ) : indisponivel ? (
            <>
              <h1 className="text-2xl font-semibold tracking-tight">Convite indisponível</h1>
              <p className="mt-1.5 text-sm text-muted-foreground">{motivo}</p>
              <Button asChild size="lg" variant="outline" className="mt-8 w-full">
                <Link to="/login">Ir para o login</Link>
              </Button>
            </>
          ) : (
            <>
              <h1 className="text-2xl font-semibold tracking-tight">Criar seu acesso</h1>
              <p className="mt-1.5 text-sm text-muted-foreground">
                {info?.organizationName ? (
                  <>
                    <strong className="text-foreground">{info.inviteeName}</strong>, você foi convidado para{" "}
                    <strong className="text-foreground">{info.organizationName}</strong>.
                  </>
                ) : (
                  "Defina uma senha para ativar sua conta."
                )}
              </p>

              <form onSubmit={enviar} className="mt-8 flex flex-col gap-4">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" value={info?.email ?? ""} disabled readOnly />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="senha">Senha</Label>
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
                  <p className="text-[11px] text-muted-foreground">Mínimo de 8 caracteres.</p>
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
                    {erro}
                  </div>
                )}

                <Button type="submit" size="lg" disabled={enviando} className="mt-1 w-full">
                  {enviando ? "Ativando..." : "Ativar acesso"}
                </Button>
              </form>

              <div className="mt-6 text-center text-sm text-muted-foreground">
                Já ativou seu acesso?{" "}
                <Link to="/login" className="font-semibold text-primary hover:underline">
                  Fazer login
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
