import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./lib/auth";
import { Landing } from "./pages/Landing";
import { Login } from "./pages/Login";

const Layout = lazy(() => import("./components/Layout").then((m) => ({ default: m.Layout })));
const Convite = lazy(() => import("./pages/Convite").then((m) => ({ default: m.Convite })));
const PortalCliente = lazy(() => import("./pages/PortalCliente").then((m) => ({ default: m.PortalCliente })));
const Dashboard = lazy(() => import("./pages/Dashboard").then((m) => ({ default: m.Dashboard })));
const Clientes = lazy(() => import("./pages/Clientes").then((m) => ({ default: m.Clientes })));
const Processos = lazy(() => import("./pages/Processos").then((m) => ({ default: m.Processos })));
const Agenda = lazy(() => import("./pages/Agenda").then((m) => ({ default: m.Agenda })));
const Financeiro = lazy(() => import("./pages/Financeiro").then((m) => ({ default: m.Financeiro })));
const Equipe = lazy(() => import("./pages/Equipe").then((m) => ({ default: m.Equipe })));
const Configuracoes = lazy(() => import("./pages/Configuracoes").then((m) => ({ default: m.Configuracoes })));
const LexoIA = lazy(() => import("./pages/LexoIA").then((m) => ({ default: m.LexoIA })));

function Protegida({ children }: { children: ReactNode }) {
  const { usuario, carregando } = useAuth();
  if (carregando) return null;
  if (!usuario) return <Navigate to="/login" replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  return (
    <Suspense fallback={null}>
      <Routes>

        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/portal/:token" element={<PortalCliente />} />
        <Route path="/convite/:token" element={<Convite />} />

        <Route path="/app" element={<Protegida><Dashboard /></Protegida>} />
        <Route path="/app/ia" element={<Protegida><LexoIA /></Protegida>} />
        <Route path="/app/clientes" element={<Protegida><Clientes /></Protegida>} />
        <Route path="/app/processos" element={<Protegida><Processos /></Protegida>} />
        <Route path="/app/agenda" element={<Protegida><Agenda /></Protegida>} />
        <Route path="/app/financeiro" element={<Protegida><Financeiro /></Protegida>} />
        <Route path="/app/equipe" element={<Protegida><Equipe /></Protegida>} />
        <Route path="/app/configuracoes" element={<Protegida><Configuracoes /></Protegida>} />

        <Route path="/app/assistente" element={<Navigate to="/app/ia" replace />} />
        <Route path="/app/peticoes" element={<Navigate to="/app/ia" replace />} />
        <Route path="/app/auditoria" element={<Navigate to="/app/configuracoes" replace />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
