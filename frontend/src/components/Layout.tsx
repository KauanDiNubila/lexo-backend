import type { ReactNode } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  Briefcase,
  CalendarDays,
  Wallet,
  UsersRound,
  ScrollText,
  Settings,
  Scale,
  Sparkles,
  FileText,
} from "lucide-react";
import { useAuth } from "../lib/auth";

const navItens = [
  { para: "/app", rotulo: "Visão geral", icone: LayoutDashboard },
  { para: "/app/assistente", rotulo: "Assistente", icone: Sparkles },
  { para: "/app/peticoes", rotulo: "Petições", icone: FileText },
  { para: "/app/clientes", rotulo: "Clientes", icone: Users },
  { para: "/app/processos", rotulo: "Processos", icone: Briefcase },
  { para: "/app/agenda", rotulo: "Agenda", icone: CalendarDays },
  { para: "/app/financeiro", rotulo: "Financeiro", icone: Wallet },
  { para: "/app/equipe", rotulo: "Equipe", icone: UsersRound },
  { para: "/app/auditoria", rotulo: "Auditoria", icone: ScrollText },
  { para: "/app/configuracoes", rotulo: "Configurações", icone: Settings },
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
        <Link
          to="/"
          style={{
            padding: "0 0.75rem 1.25rem",
            display: "flex",
            alignItems: "center",
            gap: 10,
            textDecoration: "none",
            color: "inherit",
          }}
        >
          <div
            style={{
              width: 34,
              height: 34,
              borderRadius: 9,
              background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
              display: "grid",
              placeItems: "center",
              color: "white",
              boxShadow: "0 6px 16px -6px #6366f1",
            }}
          >
            <Scale size={19} strokeWidth={2.3} />
          </div>
          <span style={{ fontWeight: 700, fontSize: 18, letterSpacing: -0.3 }}>Lexo</span>
        </Link>

        <nav style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          {navItens.map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.para === "/app"}
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
                background: isActive ? "linear-gradient(100deg, var(--color-primary-soft), transparent)" : "transparent",
                boxShadow: isActive ? "inset 3px 0 0 var(--color-primary)" : "none",
              })}
            >
              <item.icone size={17} strokeWidth={2} />
              {item.rotulo}
            </NavLink>
          ))}
        </nav>
      </aside>

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
              background: "linear-gradient(135deg,#6366f1,#8b5cf6)",
              color: "white",
              display: "grid",
              placeItems: "center",
              fontWeight: 700,
              fontSize: 13,
              boxShadow: "0 4px 12px -4px #6366f1",
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
