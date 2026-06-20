"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import AuthLayout from "@/components/layout/AuthLayout";

const inputClass =
  "w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2.5 text-[var(--color-text)] text-sm placeholder:text-[var(--color-muted)] focus:outline-none focus:border-[var(--color-teal)] transition-colors";

export default function RegisterPage() {
  const { login } = useAuth();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();

    const res = await fetch("http://localhost:8080/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, email, password }),
    });

    const data = await res.json();
    login(data.token);
  }

  return (
    <AuthLayout title="Criar conta" description="Comece a controlar suas finanças">
      <form onSubmit={handleRegister} className="space-y-5">
        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Nome
          </label>
          <input
            type="text"
            placeholder="Seu nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Email
          </label>
          <input
            type="email"
            placeholder="seu@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </div>

        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Senha
          </label>
          <input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </div>

        <button
          type="submit"
          className="w-full py-3 rounded-lg bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white font-semibold text-sm transition-colors cursor-pointer mt-2"
        >
          Criar conta
        </button>
      </form>

      <p className="text-center text-xs text-[var(--color-muted)] mt-8">
        Já tem uma conta?{" "}
        <Link
          href="/login"
          className="text-[var(--color-teal)] hover:underline font-medium"
        >
          Entrar
        </Link>
      </p>
    </AuthLayout>
  );
}
