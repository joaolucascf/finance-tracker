"use client";

import { useEffect } from "react";

type ToastProps = {
  message: string;
  onClose: () => void;
  variant?: "error" | "success";
  duration?: number;
};

export function Toast({
  message,
  onClose,
  variant = "error",
  duration = 4000,
}: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [onClose, duration]);

  const accent =
    variant === "error" ? "var(--color-expense)" : "var(--color-teal)";

  return (
    <div className="fixed bottom-5 right-5 z-[60] animate-[toast-in_0.2s_ease-out]">
      <div
        className="flex items-start gap-3 max-w-sm rounded-xl border bg-[var(--color-surface)] shadow-2xl px-4 py-3"
        style={{
          borderColor: `color-mix(in srgb, ${accent} 35%, var(--color-border))`,
        }}
        role="alert"
      >
        <span
          className="mt-1.5 w-2 h-2 rounded-full flex-shrink-0"
          style={{ backgroundColor: accent }}
        />
        <p className="text-[var(--color-text)] text-sm flex-1">{message}</p>
        <button
          onClick={onClose}
          className="text-[var(--color-muted)] hover:text-[var(--color-text)] transition-colors cursor-pointer text-sm leading-none mt-0.5"
          aria-label="Fechar"
        >
          ✕
        </button>
      </div>
    </div>
  );
}
