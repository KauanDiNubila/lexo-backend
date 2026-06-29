import type { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";

const navItens = [
  { para: "/", rotulo: "Visão geral", icone: "▦" },
  { para: "/clientes", rotulo: "Clientes", icone: "👥" },
  { para: "/processos", rotulo: "Processos", icone: "📂" },
  { para: "/agenda", rotulo: "Agenda", icone: "📅" },
  { para: "/financeiro", rotulo: "Financeiro", icone: "💰" },
];

export function Layout({ children }: { children: ReactNode }) {
  const { usuario, sair } = useAuth();
  const navigate = useNavigate();

  function logout() {
    sair();
    navigate("/login");
  }

  const iniciais = (usuario?.name || "?")
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <div style={{ display: "flex", minHeight: "100vh" }}>
      {/* Sidebar */}
      <aside
        style={{
          width: 240,
          borderRight: "1px solid var(--color-border)",
          backgroundColor: "var(--color-surface)",
          display: "flex",
          flexDirection: "column",
          padding: "1.25rem 0.75rem",
        }}
      >
        <div style={{ padding: "0 0.75rem 1.25rem", display: "flex", alignItems: "center", gap: 10 }}>
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 9,
              background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
              display: "grid",
              placeItems: "center",
              fontWeight: 800,
              color: "white",
            }}
          >
            L
          </div>
          <span style={{ fontWeight: 700, fontSize: 18, letterSpacing: -0.3 }}>Lexo</span>
        </div>

        <nav style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          {navItens.map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.para === "/"}
              style={({ isActive }) => ({
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "0.6rem 0.75rem",
                borderRadius: 9,
                fontSize: 14,
                fontWeight: 600,
                textDecoration: "none",
                color: isActive ? "var(--color-text)" : "var(--color-text-muted)",
                backgroundColor: isActive ? "var(--color-primary-soft)" : "transparent",
              })}
            >
              <span style={{ width: 18, textAlign: "center" }}>{item.icone}</span>
              {item.rotulo}
            </NavLink>
          ))}
        </nav>

        <div style={{ marginTop: "auto", padding: "0 0.25rem" }}>
          <div style={{ fontSize: 12, color: "var(--color-text-muted)", padding: "0 0.5rem 0.5rem" }}>
            Microserviços conectados ✓
          </div>
        </div>
      </aside>

      {/* Conteúdo */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <header
          style={{
            height: 60,
            borderBottom: "1px solid var(--color-border)",
            display: "flex",
            alignItems: "center",
            justifyContent: "flex-end",
            padding: "0 1.5rem",
            gap: 14,
          }}
        >
          <div style={{ textAlign: "right", lineHeight: 1.2 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>{usuario?.name}</div>
            <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>{usuario?.role}</div>
          </div>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: "50%",
              backgroundColor: "var(--color-primary-soft)",
              color: "var(--color-primary)",
              display: "grid",
              placeItems: "center",
              fontWeight: 700,
              fontSize: 13,
            }}
          >
            {iniciais}
          </div>
          <button className="btn btn-ghost" onClick={logout} style={{ padding: "0.4rem 0.7rem" }}>
            Sair
          </button>
        </header>

        <main style={{ padding: "2rem", flex: 1 }}>{children}</main>
      </div>
    </div>
  );
}
