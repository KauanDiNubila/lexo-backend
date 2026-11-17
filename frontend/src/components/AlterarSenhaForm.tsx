import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export function AlterarSenhaForm() {
  const { sair } = useAuth();
  const navigate = useNavigate();

  const [atual, setAtual] = useState("");
  const [nova, setNova] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (nova !== confirmacao) {
      setErro("As senhas não coincidem");
      return;
    }
    setEnviando(true);
    try {
      await api.put("/api/conta/senha", { currentPassword: atual, newPassword: nova, confirmPassword: confirmacao });
      await sair();
      navigate("/login?senha=alterada");
    } catch (err) {
      setErro((err as Error).message);
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} style={{ display: "flex", flexDirection: "column", gap: 12, maxWidth: 320 }}>
      <div>
        <label className="label" htmlFor="senha-atual">Senha atual</label>
        <input
          id="senha-atual"
          className="input"
          type="password"
          value={atual}
          onChange={(e) => setAtual(e.target.value)}
          autoComplete="current-password"
          required
        />
      </div>
      <div>
        <label className="label" htmlFor="senha-nova">Nova senha</label>
        <input
          id="senha-nova"
          className="input"
          type="password"
          value={nova}
          onChange={(e) => setNova(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
      </div>
      <div>
        <label className="label" htmlFor="senha-confirmacao">Confirmar nova senha</label>
        <input
          id="senha-confirmacao"
          className="input"
          type="password"
          value={confirmacao}
          onChange={(e) => setConfirmacao(e.target.value)}
          minLength={8}
          autoComplete="new-password"
          required
        />
      </div>

      {erro && <div style={{ fontSize: 14, color: "var(--color-danger)" }}>{erro}</div>}

      <div>
        <button className="btn btn-primary" type="submit" disabled={enviando}>
          {enviando ? "Salvando..." : "Alterar senha"}
        </button>
      </div>
      <p style={{ margin: 0, fontSize: 12, color: "var(--color-text-muted)" }}>
        Ao alterar, você sai de todos os dispositivos e entra de novo com a nova senha.
      </p>
    </form>
  );
}
