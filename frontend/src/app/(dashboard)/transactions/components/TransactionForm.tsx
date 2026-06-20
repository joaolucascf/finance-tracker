"use client";

import { useMemo, useState } from "react";
import { createTransaction } from "@/services/transactions";
import { CategorySelect } from "../../categories/CategorySelect";
import { useCategories } from "@/hooks/useCategories";
import { FormState } from "@/types/transactions";
import { ApiError } from "@/types/api-error";

type Props = {
  onSuccess: () => void;
  disabled?: boolean;
};

const initialState: FormState = {
  description: "",
  amount: "",
  type: "EXPENSE",
  categoryId: null,
  date: "",
};

const inputClass =
  "w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2.5 text-[var(--color-text)] text-sm placeholder:text-[var(--color-muted)] focus:outline-none focus:border-[var(--color-teal)] transition-colors disabled:opacity-50";

export default function TransactionForm({ onSuccess, disabled }: Props) {
  const { categories, loading } = useCategories();
  const [form, setForm] = useState<FormState>(initialState);
  const [error, setError] = useState<ApiError | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fieldErrors = useMemo(() => {
    if (!error?.fields) return {};
    const map: Record<string, string> = {};
    error.fields.forEach((f) => {
      map[f.field] = f.message;
    });
    return map;
  }, [error]);

  const isDisabled = disabled || loading || isSubmitting;

  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = e.target;
    setError((prev) => {
      if (!prev?.fields) return prev;
      return { ...prev, fields: prev.fields.filter((f) => f.field !== name) };
    });
    setForm((prev) => ({
      ...prev,
      [name]: name === "amount" ? (value === "" ? "" : Number(value)) : value,
    }));
  }

  function handleCategoryChange(value: number | null) {
    setForm((prev) => ({ ...prev, categoryId: value }));
  }

  function isApiError(err: unknown): err is ApiError {
    return (
      typeof err === "object" &&
      err !== null &&
      "message" in err &&
      typeof (err as ApiError).message === "string"
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (isSubmitting) return;
    try {
      setIsSubmitting(true);
      setError(null);
      await createTransaction({ ...form, amount: Number(form.amount) });
      onSuccess();
      setForm(initialState);
    } catch (err) {
      if (isApiError(err)) setError(err);
      else console.error("Erro inesperado: ", err);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <h2 className="text-[var(--color-text)] text-lg font-semibold mb-2">
        Nova Transação
      </h2>

      {/* Type toggle */}
      <div className="flex rounded-lg overflow-hidden border border-[var(--color-border)] text-sm font-medium">
        <button
          type="button"
          disabled={isDisabled}
          onClick={() => setForm((prev) => ({ ...prev, type: "EXPENSE" }))}
          className="flex-1 py-2.5 transition-colors cursor-pointer disabled:cursor-not-allowed"
          style={{
            backgroundColor:
              form.type === "EXPENSE"
                ? "var(--color-expense)"
                : "var(--color-raised)",
            color:
              form.type === "EXPENSE" ? "#fff" : "var(--color-secondary)",
          }}
        >
          Despesa
        </button>
        <button
          type="button"
          disabled={isDisabled}
          onClick={() => setForm((prev) => ({ ...prev, type: "INCOME" }))}
          className="flex-1 py-2.5 transition-colors cursor-pointer disabled:cursor-not-allowed"
          style={{
            backgroundColor:
              form.type === "INCOME"
                ? "var(--color-income)"
                : "var(--color-raised)",
            color:
              form.type === "INCOME" ? "#fff" : "var(--color-secondary)",
          }}
        >
          Receita
        </button>
      </div>

      {/* Description */}
      <div className="space-y-1.5">
        <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
          Descrição
        </label>
        <input
          name="description"
          placeholder="Ex: Almoço, Salário..."
          value={form.description}
          onChange={handleChange}
          disabled={isDisabled}
          className={inputClass}
        />
        {fieldErrors.description && (
          <span className="text-[var(--color-expense)] text-xs block">
            {fieldErrors.description}
          </span>
        )}
      </div>

      {/* Amount */}
      <div className="space-y-1.5">
        <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
          Valor (R$)
        </label>
        <input
          name="amount"
          type="number"
          placeholder="0,00"
          min="0"
          step="0.01"
          value={form.amount}
          onChange={handleChange}
          disabled={isDisabled}
          className={inputClass}
        />
        {fieldErrors.amount && (
          <span className="text-[var(--color-expense)] text-xs block">
            {fieldErrors.amount}
          </span>
        )}
      </div>

      {/* Category */}
      <div className="space-y-1.5">
        <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
          Categoria
        </label>
        <CategorySelect
          value={form.categoryId}
          onChange={handleCategoryChange}
          categories={categories}
          disabled={isDisabled}
        />
      </div>

      {/* Date */}
      <div className="space-y-1.5">
        <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
          Data
        </label>
        <input
          name="date"
          type="date"
          value={form.date}
          onChange={handleChange}
          disabled={isDisabled}
          className={inputClass}
        />
        {fieldErrors.date && (
          <span className="text-[var(--color-expense)] text-xs block">
            {fieldErrors.date}
          </span>
        )}
      </div>

      {error && !error.fields && (
        <p className="text-[var(--color-expense)] text-sm">{error.message}</p>
      )}

      <button
        type="submit"
        disabled={isDisabled}
        className="w-full py-3 rounded-lg bg-[var(--color-teal)] text-white font-semibold text-sm hover:bg-[var(--color-teal-dark)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer mt-2"
      >
        {isSubmitting ? "Salvando..." : "Salvar transação"}
      </button>
    </form>
  );
}
