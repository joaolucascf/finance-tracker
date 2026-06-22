import { FormState } from "@/types/transactions";
import { apiFetch } from "./api";

export async function getTransactions() {
  return apiFetch("/transactions");
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
