import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, setAccessToken, clearAccessToken } from "./api";

export type Usuario = {
  id: string;
  organizationId: string;
  role: string;
  name: string;
  email: string;
};

type AuthResponse = {
  accessToken: string;
  user: Usuario;
};

type AuthContextType = {
  usuario: Usuario | null;
  carregando: boolean;
  login: (email: string, password: string, totpCode?: string) => Promise<void>;
  registrar: (dados: RegistroDados) => Promise<void>;
  sair: () => Promise<void>;
};

export type RegistroDados = {
  organizationName: string;
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;

    async function restaurarSessao() {
      try {
        const conta = await api.post<AuthResponse>("/api/auth/refresh");
        if (ativo) aplicar(conta);
      } catch {
        if (ativo) setUsuario(null);
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    restaurarSessao();
    return () => {
      ativo = false;
    };
  }, []);

  function aplicar(resp: AuthResponse) {
    setAccessToken(resp.accessToken);
    setUsuario(resp.user);
  }

  async function login(email: string, password: string, totpCode?: string) {
    const resp = await api.post<AuthResponse>("/api/auth/login", {
      email,
      password,
      totpCode: totpCode || null,
    });
    aplicar(resp);
  }

  async function registrar(dados: RegistroDados) {
    const resp = await api.post<AuthResponse>("/api/auth/register", dados);
    aplicar(resp);
  }

  async function sair() {
    await api.post("/api/auth/logout").catch(() => undefined);
    clearAccessToken();
    setUsuario(null);
  }

  return (
    <AuthContext.Provider value={{ usuario, carregando, login, registrar, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de AuthProvider");
  return ctx;
}
