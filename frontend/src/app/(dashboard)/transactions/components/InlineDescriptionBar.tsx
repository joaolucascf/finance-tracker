"use client";

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSave: () => void;
  onCancel: () => void;
  saving: boolean;
};

export function InlineDescriptionBar({
  value,
  onChange,
  onSave,
  onCancel,
  saving,
}: Props) {
  return (
    <div className="flex items-center gap-2 flex-1">
      <input
        autoFocus
        value={value}
        disabled={saving}
        maxLength={60}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            onSave();
          }
          if (e.key === "Escape") onCancel();
        }}
        className="flex-1 min-w-0 bg-[var(--color-raised)] border border-[var(--color-teal)] rounded-md px-2 py-1 text-[var(--color-text)] text-sm focus:outline-none disabled:opacity-50"
      />
      <button
        onClick={onSave}
        disabled={saving}
        aria-label="Salvar"
        className="flex-shrink-0 p-1 rounded text-[var(--color-teal)] hover:bg-[var(--color-border)] cursor-pointer disabled:opacity-50"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </button>
      <button
        onClick={onCancel}
        disabled={saving}
        aria-label="Cancelar"
        className="flex-shrink-0 p-1 rounded text-[var(--color-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-border)] cursor-pointer disabled:opacity-50"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 6L6 18M6 6l12 12" />
        </svg>
      </button>
    </div>
  );
}
