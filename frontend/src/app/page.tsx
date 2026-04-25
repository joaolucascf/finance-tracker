"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem("token");

    if (token) {
      router.push("/transactions");
    }
  }, []);

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <div
        style={{
          textAlign: "center",
          display: "flex",
          flexDirection: "column",
          gap: "1.5rem",
        }}
      >
        <div>
          <h1 style={{ marginBottom: "0.5rem" }}>Finance Tracker</h1>
          <p>Gerencie suas finanças de forma simples.</p>
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "center",
            gap: "1rem",
          }}
        >
          <button onClick={() => router.push("/login")}>
            Login
          </button>

          <button onClick={() => router.push("/register")}>
            Registrar
          </button>
        </div>
      </div>
    </div>
  );
}