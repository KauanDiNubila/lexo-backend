import { useEffect, useMemo, useState, type FormEvent } from "react";
import { api } from "../lib/api";

type Cliente = {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  portalToken: string | null;
  createdAt: string | null;
};

const FORM_VAZIO = { nome: "", documento: "", email: "", telefone: "" };

export function Clientes() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [busca, setBusca] = useState("");

  // modal: aberto para criar (editandoId = null) ou editar (editandoId = id)
  const [aberto, setAberto] = useState(false);
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState(FORM_VAZIO);
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

  const filtrados = useMemo(() => {
    const q = busca.trim().toLowerCase();
    if (!q) return clientes;
    return clientes.filter((c) =>
      [c.name, c.document, c.email, c.phone].some((v) => v?.toLowerCase().includes(q))
    );
  }, [clientes, busca]);

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

  async function copiarPortal(c: Cliente) {
    try {
      let token = c.portalToken;
      if (!token) {
        const r = await api.post<{ token: string }>(`/api/clientes/${c.id}/portal`);
        token = r.token;
        await carregar();
      }
      const link = `${window.location.origin}/portal/${token}`;
      await navigator.clipboard.writeText(link);
      alert(`Link do portal copiado!\n\n${link}\n\nEnvie para ${c.name} acompanhar o processo.`);
    } catch {
      alert("Não foi possível gerar o link. Tente novamente.");
    }
  }

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Clientes</h1>
          <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
            {busca ? `${filtrados.length} de ${clientes.length}` : `${clientes.length} cadastrado(s)`} · via{" "}
            <code>cliente-service</code>
          </p>
        </div>
        <button className="btn btn-primary" onClick={abrirNovo}>
          + Novo cliente
        </button>
      </div>

      <div style={{ marginBottom: 14 }}>
        <input
          className="input"
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar por nome, documento, email ou telefone..."
          style={{ maxWidth: 420 }}
        />
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : filtrados.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>
            {clientes.length === 0 ? "Nenhum cliente ainda. Clique em “Novo cliente”." : "Nenhum cliente corresponde à busca."}
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
              {filtrados.map((c) => (
                <tr key={c.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>{c.name}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.document || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.email || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{c.phone || "—"}</td>
                  <td style={{ padding: "0.85rem 1.25rem", textAlign: "right", whiteSpace: "nowrap" }}>
                    <button
                      onClick={() => copiarPortal(c)}
                      title="Gerar/copiar o link do portal do cliente"
                      style={{ background: "none", border: "none", color: c.portalToken ? "var(--color-success)" : "var(--color-text-muted)", cursor: "pointer", fontSize: 13, marginRight: 14 }}
                    >
                      {c.portalToken ? "Copiar portal" : "Gerar portal"}
                    </button>
                    <button
                      onClick={() => abrirEdicao(c)}
                      style={{ background: "none", border: "none", color: "var(--color-primary)", cursor: "pointer", fontSize: 13, marginRight: 14 }}
                    >
                      Editar
                    </button>
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

      {/* Modal de criação/edição */}
      {aberto && (
        <div
          onClick={() => setAberto(false)}
          style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.6)", display: "grid", placeItems: "center", padding: 20 }}
        >
          <div className="card" onClick={(e) => e.stopPropagation()} style={{ width: 420, maxWidth: "100%", padding: "1.75rem" }}>
            <h2 style={{ fontSize: 18, fontWeight: 700, margin: "0 0 18px" }}>
              {editandoId ? "Editar cliente" : "Novo cliente"}
            </h2>
            <form onSubmit={salvar} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <div>
                <label className="label">Nome *</label>
                <input className="input" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
              </div>
              <div>
                <label className="label">CPF / CNPJ</label>
                <input className="input" value={form.documento} onChange={(e) => setForm({ ...form, documento: e.target.value })} placeholder="52998224725" />
              </div>
              <div>
                <label className="label">Email</label>
                <input className="input" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div>
                <label className="label">Telefone</label>
                <input className="input" value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
              </div>
              {erroForm && <div style={{ color: "var(--color-danger)", fontSize: 13 }}>{erroForm}</div>}
              <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", marginTop: 6 }}>
                <button type="button" className="btn btn-ghost" onClick={() => setAberto(false)}>
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={salvando}>
                  {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
