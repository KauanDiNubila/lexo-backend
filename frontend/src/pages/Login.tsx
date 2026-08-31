import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Scale, ShieldCheck } from "lucide-react";
import { useAuth } from "../lib/auth";
import { ApiError } from "../lib/api";

export function Login() {
  const { login, registrar } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [modo, setModo] = useState<"login" | "registro">(
    params.get("modo") === "registro" ? "registro" : "login"
  );
  const [passo, setPasso] = useState<"credenciais" | "totp">("credenciais");
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
    <div style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: "2rem" }}>
      <div className="card" style={{ width: 400, maxWidth: "100%", padding: "2rem" }}>
        <Link to="/" style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6, textDecoration: "none", color: "inherit", width: "fit-content" }}>
          <div
            style={{
              width: 38,
              height: 38,
              borderRadius: 10,
              background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
              display: "grid",
              placeItems: "center",
              color: "white",
              boxShadow: "0 6px 16px -6px #6366f1",
            }}
          >
            <Scale size={21} strokeWidth={2.3} />
          </div>
          <span style={{ fontWeight: 700, fontSize: 22, letterSpacing: -0.4 }}>Lexo</span>
        </Link>
        <p style={{ color: "var(--color-text-muted)", fontSize: 14, marginBottom: 22 }}>
          {emTotp
            ? "Digite o código do seu app autenticador."
            : modo === "login"
              ? "Acesse a plataforma do seu escritório."
              : "Crie a conta do seu escritório."}
        </p>

        <form onSubmit={enviar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {emTotp ? (
            <>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 10,
                  fontSize: 13,
                  color: "var(--color-text-muted)",
                  background: "rgba(99,102,241,0.08)",
                  border: "1px solid rgba(99,102,241,0.25)",
                  borderRadius: 8,
                  padding: "0.6rem 0.75rem",
                }}
              >
                <ShieldCheck size={18} />
                <span>
                  Verificação em dois fatores ativada para <strong>{email}</strong>.
                </span>
              </div>
              <div>
                <label className="label">Código de verificação</label>
                <input
                  className="input"
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
                  <div>
                    <label className="label">Nome do escritório</label>
                    <input className="input" value={organizacao} onChange={(e) => setOrganizacao(e.target.value)} required />
                  </div>
                  <div>
                    <label className="label">Seu nome</label>
                    <input className="input" value={nome} onChange={(e) => setNome(e.target.value)} required />
                  </div>
                </>
              )}
              <div>
                <label className="label">Email</label>
                <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </div>
              <div>
                <label className="label">Senha</label>
                <input className="input" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required />
              </div>
            </>
          )}

          {erro && (
            <div
              style={{
                fontSize: 13,
                color: "var(--color-danger)",
                background: "rgba(248,113,113,0.1)",
                border: "1px solid rgba(248,113,113,0.3)",
                borderRadius: 8,
                padding: "0.55rem 0.75rem",
              }}
            >
              {erro}
            </div>
          )}

          <button
            className="btn btn-primary"
            type="submit"
            disabled={enviando || (emTotp && codigo.length < 6)}
            style={{ marginTop: 4 }}
          >
            {enviando ? "Aguarde..." : emTotp ? "Verificar" : modo === "login" ? "Entrar" : "Criar conta"}
          </button>
        </form>

        <div style={{ marginTop: 18, textAlign: "center", fontSize: 13, color: "var(--color-text-muted)" }}>
          {emTotp ? (
            <button
              onClick={voltarParaCredenciais}
              style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontWeight: 600 }}
            >
              Voltar
            </button>
          ) : (
            <>
              {modo === "login" ? "Não tem conta?" : "Já tem conta?"}{" "}
              <button
                onClick={trocarModo}
                style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontWeight: 600 }}
              >
                {modo === "login" ? "Criar escritório" : "Fazer login"}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
