"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import AuthLayout from "@/components/layout/AuthLayout";

const inputClass = (error?: string) =>
  `w-full bg-[var(--color-raised)] border rounded-lg px-3 py-2.5 text-[var(--color-text)] text-sm placeholder:text-[var(--color-muted)] focus:outline-none transition-colors ${
    error
      ? "border-[var(--color-expense)] focus:border-[var(--color-expense)]"
      : "border-[var(--color-border)] focus:border-[var(--color-teal)]"
  }`;

type Errors = { name?: string; email?: string; password?: string; general?: string };

function validate(name: string, email: string, password: string): Errors {
  const errors: Errors = {};
  if (!name.trim()) errors.name = "Nome obrigatório";
  if (!email.trim()) errors.email = "Email obrigatório";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "Email inválido";
  if (!password) errors.password = "Senha obrigatória";
  else if (password.length < 6) errors.password = "Mínimo de 6 caracteres";
  return errors;
}

export default function RegisterPage() {
  const { login } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Errors>({});
  const [loading, setLoading] = useState(false);

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault();

    const validation = validate(name, email, password);
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setErrors({});
    setLoading(true);

    try {
      const res = await fetch("http://localhost:8080/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        setErrors({ general: data.message ?? "Erro ao criar conta" });
        return;
      }

      login(data.token, data.refreshToken);
    } catch {
      setErrors({ general: "Não foi possível conectar ao servidor. Tente novamente." });
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout title="Criar conta" description="Comece a controlar suas finanças">
      <form onSubmit={handleRegister} className="space-y-5">
        {errors.general && (
          <p className="text-[var(--color-expense)] text-sm text-center">{errors.general}</p>
        )}

        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Nome
          </label>
          <input
            type="text"
            placeholder="Seu nome"
            value={name}
            onChange={(e) => { setName(e.target.value); setErrors((p) => ({ ...p, name: undefined })); }}
            className={inputClass(errors.name)}
          />
          {errors.name && <p className="text-[var(--color-expense)] text-xs">{errors.name}</p>}
        </div>

        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Email
          </label>
          <input
            type="email"
            placeholder="seu@email.com"
            value={email}
            onChange={(e) => { setEmail(e.target.value); setErrors((p) => ({ ...p, email: undefined })); }}
            className={inputClass(errors.email)}
          />
          {errors.email && <p className="text-[var(--color-expense)] text-xs">{errors.email}</p>}
        </div>

        <div className="space-y-1.5">
          <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
            Senha
          </label>
          <input
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => { setPassword(e.target.value); setErrors((p) => ({ ...p, password: undefined })); }}
            className={inputClass(errors.password)}
          />
          {errors.password && <p className="text-[var(--color-expense)] text-xs">{errors.password}</p>}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 rounded-lg bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed mt-2"
        >
          {loading ? "Criando conta..." : "Criar conta"}
        </button>
      </form>

      <p className="text-center text-xs text-[var(--color-muted)] mt-8">
        Já tem uma conta?{" "}
        <Link href="/login" className="text-[var(--color-teal)] hover:underline font-medium">
          Entrar
        </Link>
      </p>
    </AuthLayout>
  );
}
