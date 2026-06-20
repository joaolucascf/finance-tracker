"use client";

import { useEffect, useState } from "react";

type Props = {
  value: number;
  onChange: (value: number) => void;
  disabled?: boolean;
  className?: string;
  name?: string;
};

const MAX_CENTS = 999_999_999; // R$ 9.999.999,99

function formatCents(cents: number): string {
  const intPart = Math.floor(cents / 100);
  const decPart = cents % 100;
  const intStr = intPart.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${intStr},${String(decPart).padStart(2, "0")}`;
}

export function MoneyInput({ value, onChange, disabled, className, name }: Props) {
  const [cents, setCents] = useState(() => Math.round(value * 100));

  useEffect(() => {
    setCents(Math.round(value * 100));
  }, [value]);

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Tab") return;
    if (e.metaKey || e.ctrlKey) return;
    e.preventDefault();

    if (e.key === "Backspace" || e.key === "Delete") {
      const next = Math.floor(cents / 10);
      setCents(next);
      onChange(next / 100);
      return;
    }

    if (/^\d$/.test(e.key)) {
      const digit = parseInt(e.key, 10);
      const next = Math.min(cents * 10 + digit, MAX_CENTS);
      setCents(next);
      onChange(next / 100);
    }
  }

  return (
    <input
      type="text"
      inputMode="numeric"
      name={name}
      value={formatCents(cents)}
      onChange={() => {}}
      onKeyDown={handleKeyDown}
      onPaste={(e) => e.preventDefault()}
      disabled={disabled}
      className={className}
    />
  );
}
