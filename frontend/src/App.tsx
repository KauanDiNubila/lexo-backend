import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./lib/auth";
import { Layout } from "./components/Layout";
import { Landing } from "./pages/Landing";
import { Login } from "./pages/Login";
import { PortalCliente } from "./pages/PortalCliente";
import { Clientes } from "./pages/Clientes";
import { Processos } from "./pages/Processos";
import { Agenda } from "./pages/Agenda";
import { Financeiro } from "./pages/Financeiro";
import { Equipe } from "./pages/Equipe";
import { Auditoria } from "./pages/Auditoria";
import { Configuracoes } from "./pages/Configuracoes";
import { Dashboard } from "./pages/Dashboard";
import { Assistente } from "./pages/Assistente";
import { Peticoes } from "./pages/Peticoes";
import type { ReactNode } from "react";

function Protegida({ children }: { children: ReactNode }) {
  const { usuario, carregando } = useAuth();
  if (carregando) return null;
  if (!usuario) return <Navigate to="/login" replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  return (
    <Routes>
      {/* Público */}
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/portal/:token" element={<PortalCliente />} />

      {/* Aplicação (protegida) */}
      <Route path="/app" element={<Protegida><Dashboard /></Protegida>} />
      <Route path="/app/assistente" element={<Protegida><Assistente /></Protegida>} />
      <Route path="/app/peticoes" element={<Protegida><Peticoes /></Protegida>} />
      <Route path="/app/clientes" element={<Protegida><Clientes /></Protegida>} />
      <Route path="/app/processos" element={<Protegida><Processos /></Protegida>} />
      <Route path="/app/agenda" element={<Protegida><Agenda /></Protegida>} />
      <Route path="/app/financeiro" element={<Protegida><Financeiro /></Protegida>} />
      <Route path="/app/equipe" element={<Protegida><Equipe /></Protegida>} />
      <Route path="/app/auditoria" element={<Protegida><Auditoria /></Protegida>} />
      <Route path="/app/configuracoes" element={<Protegida><Configuracoes /></Protegida>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
