"use client";

import { useMemo, useState } from "react";
import {
  createTransaction,
  updateTransaction,
} from "@/services/transactions";
import { CategorySelect } from "../../categories/CategorySelect";
import { useCategories } from "@/hooks/useCategories";
import { FormState } from "@/types/transactions";
import { ApiError } from "@/types/api-error";
import { MoneyInput } from "@/components/ui/MoneyInput";

type Props = {
  onSuccess: () => void;
  disabled?: boolean;
  /** When provided, the form edits this transaction instead of creating one. */
  transactionId?: number;
  initialValues?: FormState;
  /** Imported transactions only allow editing the description. */
  imported?: boolean;
  /** Called on submit failure; when set, generic errors are delegated here instead of rendered inline. */
  onError?: (error: ApiError) => void;
};

const getInitialState = (): FormState => ({
  description: "",
  amount: 0,
  type: "EXPENSE",
  categoryId: null,
  date: (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  })(),
});

const inputClass =
  "w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2.5 text-[var(--color-text)] text-sm placeholder:text-[var(--color-muted)] focus:outline-none focus:border-[var(--color-teal)] transition-colors disabled:opacity-50";

export default function TransactionForm({
  onSuccess,
  disabled,
  transactionId,
  initialValues,
  imported = false,
  onError,
}: Props) {
  const { categories, loading, reload: reloadCategories } = useCategories();
  const [form, setForm] = useState<FormState>(
    () => initialValues ?? getInitialState(),
  );
  const [error, setError] = useState<ApiError | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isEditing = transactionId != null;

  const fieldErrors = useMemo(() => {
    if (!error?.fields) return {};
    const map: Record<string, string> = {};
    error.fields.forEach((f) => {
      map[f.field] = f.message;
    });
    return map;
  }, [error]);

  const isDisabled = disabled || loading || isSubmitting;
  // Imported transactions lock every field except the description.
  const lockedFields = imported;

  function handleChange(
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) {
    const { name, value } = e.target;
    setError((prev) => {
      if (!prev?.fields) return prev;
      return { ...prev, fields: prev.fields.filter((f) => f.field !== name) };
    });
    setForm((prev) => ({ ...prev, [name]: value }));
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
      if (isEditing) {
        await updateTransaction(transactionId, form);
      } else {
        await createTransaction(form);
      }
      onSuccess();
      if (!isEditing) setForm(getInitialState());
    } catch (err) {
      if (isApiError(err)) {
        // Field-level errors stay inline; generic errors can be delegated (e.g. a toast).
        if (err.fields || !onError) setError(err);
        else onError(err);
      } else {
        console.error("Erro inesperado: ", err);
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <h2 className="text-[var(--color-text)] text-lg font-semibold mb-2">
        {isEditing ? "Editar Transação" : "Nova Transação"}
      </h2>

      {lockedFields && (
        <p className="text-[var(--color-muted)] text-xs">
          Transação importada — apenas a descrição pode ser editada.
        </p>
      )}

      {/* Type toggle */}
      <div className="flex rounded-lg overflow-hidden border border-[var(--color-border)] text-sm font-medium">
        <button
          type="button"
          disabled={isDisabled || lockedFields}
          onClick={() => setForm((prev) => ({ ...prev, type: "EXPENSE" }))}
          className="flex-1 py-2.5 transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
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
          disabled={isDisabled || lockedFields}
          onClick={() => setForm((prev) => ({ ...prev, type: "INCOME" }))}
          className="flex-1 py-2.5 transition-colors cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
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
        <MoneyInput
          name="amount"
          value={form.amount}
          onChange={(v) => {
            setError((prev) => {
              if (!prev?.fields) return prev;
              return { ...prev, fields: prev.fields.filter((f) => f.field !== "amount") };
            });
            setForm((prev) => ({ ...prev, amount: v }));
          }}
          disabled={isDisabled || lockedFields}
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
          onCategoryCreated={reloadCategories}
          disabled={isDisabled || lockedFields}
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
          disabled={isDisabled || lockedFields}
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
        {isSubmitting
          ? "Salvando..."
          : isEditing
            ? "Salvar alterações"
            : "Salvar transação"}
      </button>
    </form>
  );
}
