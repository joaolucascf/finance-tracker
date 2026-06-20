"use client";

import { useState } from "react";
import TransactionForm from "./components/TransactionForm";
import { useTransactions } from "@/hooks/useTransactions";
import { Modal } from "@/components/ui/Modal";
import { Transaction } from "@/types/transactions";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

function formatDate(dateStr: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
  }).format(new Date(dateStr + "T00:00:00"));
}

export default function TransactionsPage() {
  const { transactions, loading, error, reload } = useTransactions();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const totalIncome = transactions
    .filter((t) => t.type === "INCOME")
    .reduce((s, t) => s + t.amount, 0);
  const totalExpense = transactions
    .filter((t) => t.type === "EXPENSE")
    .reduce((s, t) => s + t.amount, 0);
  const balance = totalIncome - totalExpense;

  return (
    <div className="space-y-8">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">
          Transações
        </h1>
        <button
          onClick={() => setIsCreateModalOpen(true)}
          className="flex items-center gap-1.5 bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors cursor-pointer"
        >
          <span className="text-base leading-none">+</span>
          Nova transação
        </button>
      </div>

      {/* Summary cards */}
      {!loading && !error && (
        <div className="grid grid-cols-3 gap-4">
          <div className="rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] p-5">
            <p className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium mb-3">
              Saldo
            </p>
            <p
              className="text-xl font-bold"
              style={{
                color:
                  balance >= 0
                    ? "var(--color-income)"
                    : "var(--color-expense)",
              }}
            >
              {formatCurrency(balance)}
            </p>
          </div>
          <div className="rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] p-5">
            <p className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium mb-3">
              Receitas
            </p>
            <p className="text-xl font-bold text-[var(--color-income)]">
              {formatCurrency(totalIncome)}
            </p>
          </div>
          <div className="rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] p-5">
            <p className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium mb-3">
              Despesas
            </p>
            <p className="text-xl font-bold text-[var(--color-expense)]">
              {formatCurrency(totalExpense)}
            </p>
          </div>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="flex items-center justify-center py-20">
          <p className="text-[var(--color-muted)] text-sm">Carregando...</p>
        </div>
      )}

      {/* Error */}
      {error && (
        <div
          className="rounded-xl border p-4 text-sm"
          style={{
            borderColor: "color-mix(in srgb, var(--color-expense) 30%, transparent)",
            backgroundColor: "color-mix(in srgb, var(--color-expense) 10%, transparent)",
            color: "var(--color-expense)",
          }}
        >
          {error}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && transactions.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-12 h-12 rounded-full bg-[var(--color-surface)] border border-[var(--color-border)] flex items-center justify-center mb-4">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path
                d="M10 4v12M4 10h12"
                stroke="var(--color-muted)"
                strokeWidth="1.5"
                strokeLinecap="round"
              />
            </svg>
          </div>
          <p className="text-[var(--color-secondary)] text-sm font-medium">
            Nenhuma transação ainda
          </p>
          <p className="text-[var(--color-muted)] text-xs mt-1">
            Clique em "Nova transação" para começar.
          </p>
        </div>
      )}

      {/* Transaction list */}
      {!loading && !error && transactions.length > 0 && (
        <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] overflow-hidden">
          {transactions.map((t, i) => (
            <TransactionItem
              key={t.id}
              transaction={t}
              last={i === transactions.length - 1}
            />
          ))}
        </div>
      )}

      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      >
        <TransactionForm
          onSuccess={() => {
            setIsCreateModalOpen(false);
            reload();
          }}
        />
      </Modal>
    </div>
  );
}

function TransactionItem({
  transaction: t,
  last,
}: {
  transaction: Transaction;
  last: boolean;
}) {
  const isIncome = t.type === "INCOME";

  return (
    <div
      className={`flex items-center gap-4 px-5 py-4 hover:bg-[var(--color-raised)] transition-colors ${
        !last ? "border-b border-[var(--color-border)]" : ""
      }`}
    >
      {/* Color dot */}
      <div
        className="w-2 h-2 rounded-full flex-shrink-0"
        style={{
          backgroundColor: isIncome
            ? "var(--color-income)"
            : "var(--color-expense)",
        }}
      />

      {/* Description + category */}
      <div className="flex-1 min-w-0">
        <p className="text-[var(--color-text)] text-sm font-medium truncate">
          {t.description}
        </p>
        {t.category && (
          <span className="text-[var(--color-muted)] text-xs">
            {t.category.name}
          </span>
        )}
      </div>

      {/* Date */}
      <span className="text-[var(--color-muted)] text-xs flex-shrink-0">
        {formatDate(t.date)}
      </span>

      {/* Amount */}
      <span
        className="text-sm font-semibold flex-shrink-0 w-28 text-right"
        style={{
          color: isIncome ? "var(--color-income)" : "var(--color-expense)",
        }}
      >
        {isIncome ? "+" : "−"}
        {formatCurrency(t.amount)}
      </span>
    </div>
  );
}
