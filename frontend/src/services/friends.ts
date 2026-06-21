import { apiFetch } from "./api";

export async function getFriends() {
  return apiFetch("/friends");
}

export async function getFriendRequests() {
  return apiFetch("/friends/requests");
}

export async function sendFriendRequest(email: string) {
  return apiFetch("/friends/requests", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export async function acceptFriendRequest(id: number) {
  return apiFetch(`/friends/requests/${id}/accept`, {
    method: "POST",
  });
}

export async function rejectFriendRequest(id: number) {
  return apiFetch(`/friends/requests/${id}/reject`, {
    method: "POST",
  });
}
