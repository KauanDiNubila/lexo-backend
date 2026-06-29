import { useEffect, useState, type FormEvent } from "react";
import { api } from "../lib/api";

type Cliente = {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  createdAt: string | null;
};

export function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [aberto, setAberto] = useState(false);

  // form
  const [nome, setNome] = useState("");
  const [documento, setDocumento] = useState("");
  const [email, setEmail] = useState("");
  const [telefone, setTelefone] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState<string | null>(null);

  async function carregar() {
    setCarregando(true);
    setErro(null);
    try {
      setClientes(await api.get<Cliente[]>("/api/clientes"));
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  async function criar(e: FormEvent) {
    e.preventDefault();
    setErroForm(null);
    setSalvando(true);
    try {
      await api.post("/api/clientes", {
        name: nome,
        document: documento || null,
        email: email || null,
        phone: telefone || null,
      });
      setNome("");
      setDocumento("");
      setEmail("");
      setTelefone("");
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

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Clientes</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {clientes.length} cadastrado(s) · via <code>cliente-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setAberto(true)}>
          + Novo cliente
        </button>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : clientes.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>
            Nenhum cliente ainda. Clique em “Novo cliente”.
          </div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>NOME</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>DOCUMENTO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>EMAIL</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>TELEFONE</th>
                <th style={{ padding: "0.85rem 1.25rem" }}></th>
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>{c.name}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.document || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.email || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.phone || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", textAlign: "right" }}>
                    <button
                      onClick={() => excluir(c.id)}
                      style={{ background: "none", border: "none", color: "var(--color-danger)", cursor: "pointer", fontSize: 13 }}
                    >
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal de criação */}
      {aberto && (
        <div
          onClick={() => setAberto(false)}
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}
        >
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 420, maxWidth: "100%", padding: "1.75rem" }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>Novo cliente</h2>
            <form onSubmit={criar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Nome *</label>
                <input className="input" value={nome} onChange={(e) => setNome(e.target.value)} required />
              </div>
              <div>
                <label className="label">CPF / CNPJ</label>
                <input className="input" value={documento} onChange={(e) => setDocumento(e.target.value)} placeholder="52998224725" />
              </div>
              <div>
                <label className="label">Email</label>
                <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
              <div>
                <label className="label">Telefone</label>
                <input className="input" value={telefone} onChange={(e) => setTelefone(e.target.value)} />
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>
                  {salvando ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
