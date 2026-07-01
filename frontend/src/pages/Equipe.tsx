import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

type Usuario = { id: string; name: string; email: string; role: string; totpEnabled: boolean };
type Convite = { id: string; name: string; email: string; role: string };

const PAPEIS = ["ADMIN", "ADVOGADO", "SECRETARIA"];

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
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Equipe</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {usuarios.length} usuário(s) · via <code>auth-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setAberto(true)}>+ Convidar</button>
      </div>

      {erro && <div style={{ color: "var(--color-danger)", marginBottom: 16 }}>{erro}</div>}

      <div className="card" style={{ overflow: "hidden", marginBottom: 22 }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>NOME</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>EMAIL</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>PAPEL</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>2FA</th>
                <th style={{ padding: "0.85rem 1.25rem" }}></th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>
                    {u.name} {u.id === eu?.id && <span style={{ color: "var(--color-primary)", fontSize: 12 }}>(você)</span>}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{u.email}</td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    {u.id === eu?.id ? (
                      <span style={{ color: "var(--color-text-muted)" }}>{u.role}</span>
                    ) : (
                      <select className="input" style={{ width: "auto", padding: "0.3rem 0.5rem" }} value={u.role} onChange={(e) => mudarPapel(u.id, e.target.value)}>
                        {PAPEIS.map((p) => (<option key={p} value={p}>{p}</option>))}
                      </select>
                    )}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    {u.totpEnabled ? <span style={{ color: "#34d399" }}>●</span> : <span style={{ color: "var(--color-text-muted)" }}>○</span>}
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem", textAlign: "right" }}>
                    {u.id !== eu?.id && (
                      <button onClick={() => remover(u.id)} style={{ background: "none", border: "none", color: "var(--color-danger)", cursor: "pointer", fontSize: 13 }}>Remover</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {convites.length > 0 && (
        <>
          <h2 style={{ fontSize: 15, fontWeight: 600, color: "var(--color-text-muted)", margin: "0 0 10px" }}>Convites pendentes</h2>
          <div className="card" style={{ overflow: "hidden" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <tbody>
                {convites.map((c) => (
                  <tr key={c.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                    <td style={{ padding: "0.7rem 1.25rem", fontWeight: 600 }}>{c.name}</td>
                    <td style={{ padding: "0.7rem 1.25rem", color: "var(--color-text-muted)" }}>{c.email}</td>
                    <td style={{ padding: "0.7rem 1.25rem", color: "var(--color-text-muted)" }}>{c.role}</td>
                    <td style={{ padding: "0.7rem 1.25rem", textAlign: "right" }}>
                      <button onClick={() => revogar(c.id)} style={{ background: "none", border: "none", color: "var(--color-danger)", cursor: "pointer", fontSize: 13 }}>Revogar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {aberto && (
        <div onClick={() => setAberto(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}>
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 420, maxWidth: "100%", padding: "1.75rem" }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>Convidar para a equipe</h2>
            <form onSubmit={convidar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div><label className="label">Nome *</label><input className="input" value={nome} onChange={(e) => setNome(e.target.value)} required /></div>
              <div><label className="label">Email *</label><input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
              <div>
                <label className="label">Papel</label>
                <select className="input" value={papel} onChange={(e) => setPapel(e.target.value)}>
                  {PAPEIS.map((p) => (<option key={p} value={p}>{p}</option>))}
                </select>
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>Cancelar</button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>{salvando ? "Enviando..." : "Enviar convite"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
