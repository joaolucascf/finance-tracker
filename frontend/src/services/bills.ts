import { apiFetch } from "./api";

export async function renameBill(id: number, name: string) {
  return apiFetch(`/bills/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
}
