import { apiFetch } from "./api";

export async function getCategories() {
  return apiFetch("/categories");
}
