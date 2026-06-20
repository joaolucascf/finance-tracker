import { logout, tryRefreshToken } from "@/lib/auth";
import { ApiError } from "@/types/api-error";

const API_URL = "http://localhost:8080";

let refreshing: Promise<boolean> | null = null;

export async function apiFetch(endpoint: string, options: RequestInit = {}, retry = true) {
  const token = localStorage.getItem("token");

  const response: Response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
  });

  if (response.status === 401 && retry) {
    if (!refreshing) {
      refreshing = tryRefreshToken().finally(() => { refreshing = null; });
    }

    const refreshed = await refreshing;

    if (refreshed) {
      return apiFetch(endpoint, options, false);
    }

    logout();
    throw { message: "Sessão expirada" } as ApiError;
  }

  let data = null;

  try {
    data = await response.json();
  } catch {}

  if (!response.ok) {
    const error: ApiError = {
      message: data?.message || "Erro na requisição",
      fields: data?.errors,
    };

    throw error;
  }

  return data;
}
