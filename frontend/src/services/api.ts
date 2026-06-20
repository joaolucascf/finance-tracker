import { logout } from "@/lib/auth";
import { ApiError } from "@/types/api-error";

const API_URL = "http://localhost:8080";

export async function apiFetch(endpoint: string, options: RequestInit = {}) {
  const token = localStorage.getItem("token");

  const response: Response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
  });

  if (response.status === 401) {
    logout();
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
