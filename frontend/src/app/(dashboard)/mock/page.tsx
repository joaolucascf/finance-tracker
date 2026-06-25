"use client";

import Link from "next/link";
import { CONCEPTS } from "./concepts";

export default function MockIndexPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">
          Mock de dados
        </h1>
        <p className="text-[var(--color-secondary)] text-sm mt-1">
          CRUD interno para criar dados importados na mão (apenas para testes).
        </p>
      </div>

      <div className="grid gap-3">
        {Object.values(CONCEPTS).map((c) => (
          <Link
            key={c.path}
            href={`/mock/${c.path}`}
            className="block rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 hover:border-[var(--color-teal)] transition-colors"
          >
            <p className="text-[var(--color-text)] text-sm font-semibold">
              {c.label}
            </p>
            <p className="text-[var(--color-muted)] text-xs mt-0.5">{c.hint}</p>
          </Link>
        ))}
      </div>

      <p className="text-[var(--color-muted)] text-xs">
        Ordem sugerida: Conexão → Conta (CARTAO_CREDITO) → Fatura → Transações
        (vinculadas pelo Bill ID).
      </p>
    </div>
  );
}
