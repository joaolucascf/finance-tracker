"use client";

import { useEffect, useRef, useState } from "react";

export type SelectOption = {
  value: string | number;
  label: string;
};

export type SelectAction = {
  label: string;
  onSelect: () => void;
  className?: string;
};

type Props = {
  value: string | number | null;
  onChange: (value: string | number | null) => void;
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
  actions?: SelectAction[];
};

export function Select({
  value,
  onChange,
  options,
  placeholder,
  disabled,
  actions,
}: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  const selectedLabel = options.find((o) => o.value === value)?.label;

  function select(val: string | number | null) {
    onChange(val);
    setOpen(false);
  }

  function triggerAction(action: SelectAction) {
    setOpen(false);
    action.onSelect();
  }

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        disabled={disabled}
        className={`w-full flex items-center justify-between bg-[var(--color-raised)] border rounded-lg px-3 py-2.5 text-sm transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
          open ? "border-[var(--color-teal)]" : "border-[var(--color-border)]"
        }`}
      >
        <span
          className={
            selectedLabel
              ? "text-[var(--color-text)]"
              : "text-[var(--color-muted)]"
          }
        >
          {selectedLabel ?? (placeholder ?? "Selecionar")}
        </span>
        <svg
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
          className={`flex-shrink-0 ml-2 text-[var(--color-muted)] transition-transform ${open ? "rotate-180" : ""}`}
        >
          <path
            d="M2 4L6 8L10 4"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <div className="absolute z-20 left-0 right-0 top-full mt-1 rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-xl overflow-hidden">
          <div className="py-1 max-h-56 overflow-y-auto">
            <button
              type="button"
              onClick={() => select(null)}
              className={`w-full text-left px-4 py-2.5 text-sm transition-colors hover:bg-[var(--color-raised)] ${
                value === null
                  ? "text-[var(--color-teal)]"
                  : "text-[var(--color-secondary)] hover:text-[var(--color-text)]"
              }`}
            >
              {placeholder ?? "Selecionar"}
            </button>

            {options.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => select(opt.value)}
                className={`w-full text-left px-4 py-2.5 text-sm transition-colors hover:bg-[var(--color-raised)] ${
                  value === opt.value
                    ? "text-[var(--color-teal)]"
                    : "text-[var(--color-secondary)] hover:text-[var(--color-text)]"
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {actions && actions.length > 0 && (
            <div className="border-t border-[var(--color-border)] py-1">
              {actions.map((action) => (
                <button
                  key={action.label}
                  type="button"
                  onClick={() => triggerAction(action)}
                  className={`w-full text-left px-4 py-2.5 text-sm transition-colors hover:bg-[var(--color-raised)] ${
                    action.className ??
                    "text-[var(--color-secondary)] hover:text-[var(--color-text)]"
                  }`}
                >
                  {action.label}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
