import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

type Cliente = { id: string };

export function Dashboard() {
  const { usuario } = useAuth();
  const [totalClientes, setTotalClientes] = useState<number | null>(null);

  useEffect(() => {
    api
      .get<Cliente[]>("/api/clientes")
      .then((cs) => setTotalClientes(cs.length))
      .catch(() => setTotalClientes(null));
  }, []);

  const cards = [
    { rotulo: "Clientes", valor: totalClientes ?? "—", cor: "#6366f1", icone: "👥" },
    { rotulo: "Microserviços", valor: "6", cor: "#34d399", icone: "🧩" },
    { rotulo: "Bancos isolados", valor: "5", cor: "#fbbf24", icone: "🗄️" },
  ];

  return (
    <div>
      <h1 style={{ fontSize: 26, fontWeight: 700, margin: "0 0 4px" }}>
        Olá, {usuario?.name?.split(" ")[0]} 👋
      </h1>
      <p style={{ color: "var(--color-text-muted)", margin: "0 0 28px" }}>
        Visão geral do seu escritório — dados vindos dos microserviços via API Gateway.
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 16 }}>
        {cards.map((c) => (
          <div key={c.rotulo} className="card" style={{ padding: "1.25rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ color: "var(--color-text-muted)", fontSize: 13, fontWeight: 600 }}>{c.rotulo}</span>
              <span style={{ fontSize: 20 }}>{c.icone}</span>
            </div>
            <div style={{ fontSize: 32, fontWeight: 800, marginTop: 8, color: c.cor }}>{c.valor}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
