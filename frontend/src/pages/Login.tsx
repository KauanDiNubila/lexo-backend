import { lazy, Suspense, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ShieldCheck } from "lucide-react";
import { useAuth } from "../lib/auth";
import { ApiError } from "../lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const Silk = lazy(() => import("@/components/ui/silk"));

export function Login() {
  const { login, registrar } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [modo, setModo] = useState<"login" | "registro">(
    params.get("modo") === "registro" ? "registro" : "login"
  );
  const [passo, setPasso] = useState<"credenciais" | "totp">("credenciais");
  const [silkPronto, setSilkPronto] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [nome, setNome] = useState("");
  const [organizacao, setOrganizacao] = useState("");
  const [codigo, setCodigo] = useState("");

  function voltarParaCredenciais() {
    setPasso("credenciais");
    setCodigo("");
    setErro(null);
  }

  function trocarModo() {
    setModo(modo === "login" ? "registro" : "login");
    voltarParaCredenciais();
  }

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      if (modo === "login") {
        await login(email, senha, passo === "totp" ? codigo : undefined);
      } else {
        await registrar({
          organizationName: organizacao,
          name: nome,
          email,
          password: senha,
          confirmPassword: senha,
        });
      }
      navigate("/app");
    } catch (err) {
      if (err instanceof ApiError && err.code === "TOTP_REQUIRED") {
        setPasso("totp");
        setCodigo("");
      } else {
        setErro((err as Error).message);
      }
    } finally {
      setEnviando(false);
    }
  }

  const emTotp = modo === "login" && passo === "totp";

  return (
    <div className="grid min-h-screen md:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-[#08293d] md:flex">
        <div
          aria-hidden
          className={`pointer-events-none absolute inset-0 h-full w-full ${silkPronto ? "animate-in fade-in duration-500" : ""}`}
        >
          <Suspense fallback={null}>
            <Silk
              color="#0b3450"
              speed={5}
              scale={1.3}
              noiseIntensity={1.1}
              rotation={0.15}
              onReady={() => setSilkPronto(true)}
            />
          </Suspense>
        </div>
        <div className="relative flex h-full w-full flex-col p-12">
          <Link to="/" className="w-fit text-lg font-bold tracking-tight text-white">
            Lexo
          </Link>
          <div className="flex flex-1 flex-col justify-center">
            <p className="max-w-sm text-3xl leading-snug font-medium text-white">
              Gestão jurídica para escritórios de advocacia.
            </p>
            <p className="mt-4 max-w-sm text-sm text-white/60">
              Processos, prazos, financeiro e o portal do seu cliente, tudo num só lugar.
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

          <h1 className="text-2xl font-semibold tracking-tight">
            {emTotp ? "Verificação em duas etapas" : modo === "login" ? "Entrar" : "Criar escritório"}
          </h1>
          <p className="mt-1.5 text-sm text-muted-foreground">
            {emTotp
              ? "Digite o código do seu app autenticador."
              : modo === "login"
                ? "Acesse a plataforma do seu escritório."
                : "Crie a conta do seu escritório em poucos minutos."}
          </p>

          {params.get("convite") === "aceito" && !emTotp && (
            <div className="mt-5 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2.5 text-sm text-emerald-700 dark:text-emerald-400">
              Acesso ativado. Entre com o email do convite e a senha que você acabou de definir.
            </div>
          )}

          <form onSubmit={enviar} className="mt-8 flex flex-col gap-4">
            {emTotp ? (
              <>
                <div className="flex items-center gap-2.5 rounded-lg border border-primary/20 bg-primary/8 px-3 py-2.5 text-sm text-muted-foreground">
                  <ShieldCheck size={18} className="shrink-0 text-primary" />
                  <span>
                    Verificação em dois fatores ativada para <strong className="text-foreground">{email}</strong>.
                  </span>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="codigo">Código de verificação</Label>
                  <Input
                    id="codigo"
                    value={codigo}
                    onChange={(e) => setCodigo(e.target.value.replace(/\D/g, ""))}
                    placeholder="000000"
                    maxLength={6}
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    autoFocus
                    required
                  />
                </div>
              </>
            ) : (
              <>
                {modo === "registro" && (
                  <>
                    <div className="flex flex-col gap-1.5">
                      <Label htmlFor="organizacao">Nome do escritório</Label>
                      <Input id="organizacao" value={organizacao} onChange={(e) => setOrganizacao(e.target.value)} required />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <Label htmlFor="nome">Seu nome</Label>
                      <Input id="nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
                    </div>
                  </>
                )}
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="senha">Senha</Label>
                  <Input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
                </div>
              </>
            )}

            {erro && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
                {erro}
              </div>
            )}

            <Button type="submit" size="lg" disabled={enviando || (emTotp && codigo.length < 6)} className="mt-1 w-full">
              {enviando ? "Aguarde..." : emTotp ? "Verificar" : modo === "login" ? "Entrar" : "Criar conta"}
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-muted-foreground">
            {emTotp ? (
              <button onClick={voltarParaCredenciais} className="font-semibold text-primary hover:underline">
                Voltar
              </button>
            ) : (
              <>
                {modo === "login" ? "Não tem conta?" : "Já tem conta?"}{" "}
                <button onClick={trocarModo} className="font-semibold text-primary hover:underline">
                  {modo === "login" ? "Criar escritório" : "Fazer login"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
