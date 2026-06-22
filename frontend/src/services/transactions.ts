import { FormState } from "@/types/transactions";
import { apiFetch } from "./api";

export async function getTransactions(year?: number, month?: number) {
  const params = new URLSearchParams();
  if (year != null) params.set("year", String(year));
  if (month != null) params.set("month", String(month));
  const query = params.toString();
  return apiFetch(`/transactions${query ? `?${query}` : ""}`);
}

export async function createTransaction(data: FormState) {
  return apiFetch("/transactions", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function updateTransaction(id: number, data: FormState) {
  return apiFetch(`/transactions/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export async function deleteTransaction(id: number) {
  return apiFetch(`/transactions/${id}`, { method: "DELETE" });
}
