"use client";

import { useEffect, useRef, useState } from "react";
import { createCategory } from "@/services/categories";
import { Category } from "@/types/categories";
import { Select } from "@/components/ui/Select";

type Props = {
  value: number | null;
  onChange: (value: number | null) => void;
  categories: Category[];
  onCategoryCreated?: (category: Category) => void;
  placeholder?: string;
  disabled?: boolean;
};

export function CategorySelect({
  value,
  onChange,
  categories,
  onCategoryCreated,
  placeholder,
  disabled,
}: Props) {
  const [isCreating, setIsCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [localExtra, setLocalExtra] = useState<Category[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setLocalExtra((prev) => prev.filter((e) => !categories.some((c) => c.id === e.id)));
  }, [categories]);

  const allOptions = [...categories, ...localExtra].map((c) => ({
    value: c.id,
    label: c.name,
  }));

  function handleCancel() {
    setIsCreating(false);
    setNewName("");
    setError(null);
  }

  async function handleCreate() {
    const name = newName.trim();
    if (!name) {
      setError("Nome obrigatório");
      inputRef.current?.focus();
      return;
    }
    try {
      setIsSubmitting(true);
      setError(null);
      const created: Category = await createCategory(name);
      setLocalExtra((prev) => [...prev, created]);
      onChange(created.id);
      onCategoryCreated?.(created);
      setIsCreating(false);
      setNewName("");
    } catch {
      setError("Erro ao criar categoria");
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      handleCreate();
    }
    if (e.key === "Escape") {
      handleCancel();
    }
  }

  return (
    <div className="space-y-2">
      <Select
        value={value}
        onChange={(val) => onChange(val as number | null)}
        options={allOptions}
        placeholder={placeholder ?? "Sem categoria"}
        disabled={disabled || isCreating}
        actions={[
          {
            label: "+ Nova Categoria",
            onSelect: () => {
              setIsCreating(true);
              setNewName("");
              setError(null);
              setTimeout(() => inputRef.current?.focus(), 0);
            },
            className: "text-[var(--color-teal)] hover:bg-[var(--color-raised)]",
          },
        ]}
      />

      {isCreating && (
        <div className="flex gap-2">
          <input
            ref={inputRef}
            value={newName}
            onChange={(e) => {
              setNewName(e.target.value);
              setError(null);
            }}
            onKeyDown={handleKeyDown}
            placeholder="Nome da categoria"
            disabled={isSubmitting}
            className="flex-1 bg-[var(--color-raised)] border border-[var(--color-teal)] rounded-lg px-3 py-2 text-sm text-[var(--color-text)] placeholder:text-[var(--color-muted)] focus:outline-none disabled:opacity-50"
          />
          <button
            type="button"
            onClick={handleCreate}
            disabled={isSubmitting}
            className="px-3 py-2 rounded-lg bg-[var(--color-teal)] text-white text-sm font-medium hover:bg-[var(--color-teal-dark)] disabled:opacity-50 transition-colors cursor-pointer"
          >
            {isSubmitting ? "..." : "Criar"}
          </button>
          <button
            type="button"
            onClick={handleCancel}
            disabled={isSubmitting}
            className="px-3 py-2 rounded-lg border border-[var(--color-border)] text-[var(--color-secondary)] text-sm hover:border-[var(--color-teal)] transition-colors cursor-pointer disabled:opacity-50"
          >
            ✕
          </button>
        </div>
      )}

      {error && (
        <span className="text-[var(--color-expense)] text-xs block">{error}</span>
      )}
    </div>
  );
}
