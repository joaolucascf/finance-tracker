"use client";

import { logout } from "@/lib/auth";

export default function Header() {
  return (
    <header className="sticky top-0 z-10 flex items-center justify-between px-8 py-5 border-b border-[var(--color-border)] bg-[var(--color-surface)]">
      <div className="flex items-center gap-2.5">
        <div className="w-7 h-7 rounded-lg bg-[var(--color-teal)] flex items-center justify-center">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 10L7 4L12 10" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
        <span className="text-[var(--color-text)] font-semibold text-base tracking-tight">
          Finance
        </span>
      </div>

      <button
        onClick={logout}
        className="text-[var(--color-secondary)] text-sm px-3 py-1.5 rounded-md border border-[var(--color-border)] hover:border-[var(--color-muted)] hover:text-[var(--color-text)] transition-colors cursor-pointer"
      >
        Sair
      </button>
    </header>
  );
}
