const BASE = import.meta.env.VITE_GATEWAY_URL || "http://localhost:8080";

const TOKEN_KEY = "lexo_token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null as T;

  const data = await res.json().catch(() => null);
  if (!res.ok) {

    if (res.status === 401 && !path.startsWith("/api/auth/")) {
      clearToken();
      if (window.location.pathname.startsWith("/app")) {
        window.location.href = "/login";
      }
    }
    const msg = (data && (data.error as string)) || `Erro ${res.status}`;
    throw new Error(msg);
  }
  return data as T;
}

export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
  put: <T>(path: string, body?: unknown) => request<T>("PUT", path, body),
  patch: <T>(path: string, body?: unknown) => request<T>("PATCH", path, body),
  del: <T>(path: string) => request<T>("DELETE", path),
};
