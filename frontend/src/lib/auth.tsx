import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, setToken, clearToken, getToken } from "./api";

export type Usuario = {
  id: string;
  organizationId: string;
  role: string;
  name: string;
  email: string;
};

type AuthResponse = {
  token: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: string;
    organizationId: string;
  };
};

type AuthContextType = {
  usuario: Usuario | null;
  carregando: boolean;
  login: (email: string, password: string, totpCode?: string) => Promise<void>;
  registrar: (dados: RegistroDados) => Promise<void>;
  sair: () => void;
};

export type RegistroDados = {
  organizationName: string;
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
};

const AuthContext = createContext<AuthContextType | null>(null);

/** Decodifica o payload do JWT (sem verificar — só para ler a identidade no cliente). */
function lerToken(token: string): Usuario | null {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    const claims = JSON.parse(json);
    return {
      id: claims.sub,
      organizationId: claims.organizationId,
      role: claims.role,
      name: claims.name,
      email: claims.email,
    };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (token) setUsuario(lerToken(token));
    setCarregando(false);
  }, []);

  function aplicar(resp: AuthResponse) {
    setToken(resp.token);
    setUsuario(lerToken(resp.token));
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

  function sair() {
    clearToken();
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
