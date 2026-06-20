type ModalProps = {
  isOpen: boolean | undefined;
  onClose: () => void;
  children: React.ReactNode;
};

export function Modal({ isOpen, onClose, children }: ModalProps) {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="relative w-full max-w-md rounded-2xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 w-7 h-7 flex items-center justify-center rounded-md text-[var(--color-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-raised)] transition-colors cursor-pointer text-sm"
          aria-label="Fechar"
        >
          ✕
        </button>
        <div className="p-8">{children}</div>
      </div>
    </div>
  );
}
