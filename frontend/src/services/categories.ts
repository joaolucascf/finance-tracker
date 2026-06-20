import { apiFetch } from "./api";

export async function getCategories() {
  return apiFetch("/categories");
}

export async function createCategory(name: string) {
  return apiFetch("/categories", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}
