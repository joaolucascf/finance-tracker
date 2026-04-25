"use client";

import { useRouter } from "next/navigation";

export function useAuth() {
  const router = useRouter();

  function login(token: string) {
    localStorage.setItem("token", token);
    router.push("/");
  }

  function logout() {
    localStorage.removeItem("token");
    router.push("/login");
  }

  function isAuthenticated() {
    return !!localStorage.getItem("token");
  }

  return { login, logout, isAuthenticated };
}