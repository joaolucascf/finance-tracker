export default function PreferencesPage() {
  return (
    <div className="space-y-8">
      <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">
        Preferências
      </h1>

      <div className="rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] p-10 flex flex-col items-center justify-center text-center gap-3">
        <div className="w-10 h-10 rounded-full bg-[var(--color-raised)] border border-[var(--color-border)] flex items-center justify-center">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <circle cx="9" cy="9" r="7" stroke="var(--color-muted)" strokeWidth="1.5" />
            <path d="M9 6v3.5M9 12h.01" stroke="var(--color-muted)" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </div>
        <p className="text-[var(--color-secondary)] text-sm font-medium">Em breve</p>
        <p className="text-[var(--color-muted)] text-xs max-w-xs">
          As preferências do app estarão disponíveis em uma próxima versão.
        </p>
      </div>
    </div>
  );
}
