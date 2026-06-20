import { apiFetch } from "./api";
import { Profile, ProfileFormState } from "@/types/profile";

const API_URL = "http://localhost:8080";

export async function getProfile(): Promise<Profile> {
  return apiFetch("/profile/me");
}

export async function updateProfile(data: ProfileFormState): Promise<Profile> {
  return apiFetch("/profile/me", {
    method: "PATCH",
    body: JSON.stringify({
      nickname: data.nickname || null,
      birthDate: data.birthDate || null,
      monthlyIncome: data.monthlyIncome || null,
      maritalStatus: data.maritalStatus || null,
    }),
  });
}

export async function uploadPhoto(file: File): Promise<Profile> {
  const token = localStorage.getItem("token");
  const formData = new FormData();
  formData.append("photo", file);

  const response = await fetch(`${API_URL}/profile/me/photo`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) throw new Error("Erro ao fazer upload da foto");
  return response.json();
}
