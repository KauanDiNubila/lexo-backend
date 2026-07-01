import { useEffect, useState } from "react";
import { api } from "../lib/api";

type Registro = {
  id: string;
  userName: string;
  action: string;
  entityType: string | null;
  description: string;
  createdAt: string;
};

export function Auditoria() {
  const [registros, setRegistros] = useState<Registro[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Registro[]>("/api/auditoria")
      .then(setRegistros)
      .catch((e) => setErro((e as Error).message))
      .finally(() => setCarregando(false));
  }, []);

  return (
    <div>
      <div style={{ marginBottom: 22 }}>
        <h1 style={{ fontSize: 24, fontWeight: 700, margin: 0 }}>Auditoria</h1>
        <p style={{ color: "var(--color-text-muted)", margin: "4px 0 0", fontSize: 14 }}>
          {registros.length} evento(s) · consumidos do Kafka pelo <code>auditoria-service</code>
        </p>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        {carregando ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>Carregando...</div>
        ) : erro ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-danger)" }}>{erro}</div>
        ) : registros.length === 0 ? (
          <div style={{ padding: 40, textAlign: "center", color: "var(--color-text-muted)" }}>
            Nenhum evento ainda. Crie um cliente ou processo para gerar eventos.
          </div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left", color: "var(--color-text-muted)", fontSize: 12 }}>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>AÇÃO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>DESCRIÇÃO</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>POR</th>
                <th style={{ padding: "0.85rem 1.25rem", fontWeight: 600 }}>QUANDO</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((r) => (
                <tr key={r.id} style={{ borderTop: "1px solid var(--color-border)", fontSize: 14 }}>
                  <td style={{ padding: "0.85rem 1.25rem" }}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: "var(--color-primary)", background: "var(--color-primary-soft)", padding: "0.2rem 0.6rem", borderRadius: 6 }}>
                      {r.action}
                    </span>
                  </td>
                  <td style={{ padding: "0.85rem 1.25rem" }}>{r.description}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>{r.userName}</td>
                  <td style={{ padding: "0.85rem 1.25rem", color: "var(--color-text-muted)" }}>
                    {new Date(r.createdAt).toLocaleString("pt-BR")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
