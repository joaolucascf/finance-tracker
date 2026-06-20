"use client";

import { useRouter } from "next/navigation";

export function useAuth() {
  const router = useRouter();

  function login(token: string, refreshToken: string) {
    localStorage.setItem("token", token);
    localStorage.setItem("refreshToken", refreshToken);
    router.push("/transactions");
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    router.push("/login");
  }

  function isAuthenticated() {
    return !!localStorage.getItem("token");
  }

  return { login, logout, isAuthenticated };
}
