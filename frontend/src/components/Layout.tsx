import type { ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  DashboardCircleIcon,
  UserMultipleIcon,
  Briefcase03Icon,
  Calendar03Icon,
  Wallet01Icon,
  SparklesIcon,
  UserGroupIcon,
  Setting07Icon,
  Logout03Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { MacOSSidebar, type SidebarNavItem } from "@/components/ui/macos-sidebar";
import { useAuth } from "../lib/auth";
import { cn } from "@/lib/utils";

const navItens: (SidebarNavItem & { apenasAdmin: boolean })[] = [
  { to: "/app", label: "Visão geral", icon: DashboardCircleIcon, apenasAdmin: false },
  { to: "/app/clientes", label: "Clientes", icon: UserMultipleIcon, apenasAdmin: false },
  { to: "/app/processos", label: "Processos", icon: Briefcase03Icon, apenasAdmin: false },
  { to: "/app/agenda", label: "Agenda", icon: Calendar03Icon, apenasAdmin: false },
  { to: "/app/financeiro", label: "Financeiro", icon: Wallet01Icon, apenasAdmin: false },
  { to: "/app/ia", label: "Lexo IA", icon: SparklesIcon, apenasAdmin: false },
  { to: "/app/equipe", label: "Equipe", icon: UserGroupIcon, apenasAdmin: true },
];

export function Layout({ children }: { children: ReactNode }) {
  const { usuario, sair } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const itensVisiveis = navItens.filter((item) => !item.apenasAdmin || usuario?.role === "ADMIN");
  const activeIndex = itensVisiveis.findIndex((item) =>
    item.to === "/app" ? location.pathname === "/app" : location.pathname.startsWith(item.to)
  );
  const emConfiguracoes = location.pathname.startsWith("/app/configuracoes");

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
    <MacOSSidebar
      items={itensVisiveis}
      activeIndex={activeIndex}
      onSelect={(item) => navigate(item.to)}
      header={(isOpen) =>
        isOpen ? (
          <Link to="/" className="text-lg font-bold tracking-tight">
            Lexo
          </Link>
        ) : null
      }
      footer={(isOpen) => (
        <div className="flex flex-col gap-1">
          <Link
            to="/app/configuracoes"
            className={cn(
              "flex items-center gap-3 rounded-md px-2.5 py-2 text-left text-sm",
              emConfiguracoes ? "bg-muted font-semibold text-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground",
              !isOpen && "justify-center px-0"
            )}
          >
            <HugeiconsIcon icon={Setting07Icon} className="size-[18px] shrink-0" />
            {isOpen && "Configurações"}
          </Link>
          <div className="flex flex-col gap-1 border-t border-border pt-3">
            <div className={cn("flex items-center gap-2.5 px-1", !isOpen && "justify-center px-0")}>
              <div className="grid size-8 shrink-0 place-items-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                {iniciais}
              </div>
              {isOpen && (
                <div className="min-w-0 leading-tight">
                  <div className="truncate text-[13px] font-semibold">{usuario?.name}</div>
                  <div className="truncate text-[11px] text-muted-foreground">{usuario?.role}</div>
                </div>
              )}
            </div>
            <button
              onClick={logout}
              title="Sair"
              className={cn(
                "flex items-center gap-3 rounded-md px-2.5 py-2 text-left text-sm text-muted-foreground hover:bg-muted hover:text-foreground",
                !isOpen && "justify-center px-0"
              )}
            >
              <HugeiconsIcon icon={Logout03Icon} className="size-[18px] shrink-0" />
              {isOpen && "Sair"}
            </button>
          </div>
        </div>
      )}
    >
      <main className="p-8">{children}</main>
    </MacOSSidebar>
  );
}
