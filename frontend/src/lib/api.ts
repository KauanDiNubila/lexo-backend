const BASE = import.meta.env.VITE_GATEWAY_URL || "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;

  constructor(status: number, code: string | null, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

localStorage.removeItem("lexo_token");

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}
export function setAccessToken(token: string) {
  accessToken = token;
}
export function clearAccessToken() {
  accessToken = null;
}

function ehRotaDeAuth(path: string) {
  return path.startsWith("/api/auth/");
}

function deslogarLocalmente() {
  clearAccessToken();
  if (window.location.pathname.startsWith("/app")) {
    window.location.href = "/login";
  }
}

async function erroDe(res: Response): Promise<ApiError> {
  const data = await res.json().catch(() => null);
  const msg = (data && (data.error as string)) || `Erro ${res.status}`;
  const code = (data && (data.code as string)) || null;
  return new ApiError(res.status, code, msg);
}

async function executarRefresh(): Promise<string> {
  const res = await fetch(BASE + "/api/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
  });
  if (!res.ok) throw await erroDe(res);

  const data = await res.json();
  accessToken = data.accessToken as string;
  return accessToken;
}

let refreshEmAndamento: Promise<string> | null = null;

export function renovarSessao(): Promise<string> {
  refreshEmAndamento ??= executarRefresh().finally(() => {
    refreshEmAndamento = null;
  });
  return refreshEmAndamento;
}

function enviar(method: string, path: string, body: unknown, token: string | null) {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  return fetch(BASE + path, {
    method,
    headers,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res = await enviar(method, path, body, accessToken);

  if (res.status === 401 && !ehRotaDeAuth(path)) {
    try {
      const novoToken = await renovarSessao();
      res = await enviar(method, path, body, novoToken);
    } catch {
      deslogarLocalmente();
      throw new ApiError(401, "SESSAO_EXPIRADA", "Sessão expirada. Faça login novamente.");
    }
  }

  if (res.status === 204) return null as T;

  if (!res.ok) {
    if (res.status === 401 && !ehRotaDeAuth(path)) {
      deslogarLocalmente();
    }
    throw await erroDe(res);
  }

  return (await res.json().catch(() => null)) as T;
}

export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
  del: <T>(path: string) => request<T>("DELETE", path),
};

export type Pagina<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
