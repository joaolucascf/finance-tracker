import { apiFetch } from "./api";

export async function getConnectToken(): Promise<{ connectToken: string }> {
  return apiFetch("/open-finance/connect-token", { method: "POST" });
}

export async function registerConnection(data: { itemId: string; institutionName: string }) {
  return apiFetch("/open-finance/connections", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function getConnections() {
  return apiFetch("/open-finance/connections");
}

export async function disconnectConnection(id: number) {
  return apiFetch(`/open-finance/connections/${id}`, { method: "DELETE" });
}

export async function syncConnection(id: number) {
  return apiFetch(`/open-finance/connections/${id}/sync`, { method: "POST" });
}

export async function getAccounts() {
  return apiFetch("/open-finance/accounts");
}
