import { apiFetch } from "./api";

export function listMock(path: string) {
  return apiFetch(`/mock/${path}`);
}

export function createMock(path: string, body: unknown) {
  return apiFetch(`/mock/${path}`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateMock(path: string, id: number, body: unknown) {
  return apiFetch(`/mock/${path}/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function deleteMock(path: string, id: number) {
  return apiFetch(`/mock/${path}/${id}`, { method: "DELETE" });
}
