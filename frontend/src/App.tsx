import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./lib/auth";
import { Layout } from "./components/Layout";
import { Login } from "./pages/Login";
import { Clientes } from "./pages/Clientes";
import { Processos } from "./pages/Processos";
import { Agenda } from "./pages/Agenda";
import { Financeiro } from "./pages/Financeiro";
import { Equipe } from "./pages/Equipe";
import { Auditoria } from "./pages/Auditoria";
import { Configuracoes } from "./pages/Configuracoes";
import { Dashboard } from "./pages/Dashboard";
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
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Protegida><Dashboard /></Protegida>} />
      <Route path="/clientes" element={<Protegida><Clientes /></Protegida>} />
      <Route path="/processos" element={<Protegida><Processos /></Protegida>} />
      <Route path="/agenda" element={<Protegida><Agenda /></Protegida>} />
      <Route path="/financeiro" element={<Protegida><Financeiro /></Protegida>} />
      <Route path="/equipe" element={<Protegida><Equipe /></Protegida>} />
      <Route path="/auditoria" element={<Protegida><Auditoria /></Protegida>} />
      <Route path="/configuracoes" element={<Protegida><Configuracoes /></Protegida>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
