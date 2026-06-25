"use client";

import { useState } from "react";
import { Bill, Transaction } from "@/types/transactions";
import { formatCurrency, formatDay } from "@/lib/format";
import { useInlineDescription } from "./useInlineDescription";
import { InlineDescriptionBar } from "./InlineDescriptionBar";

type Props = {
  bill: Bill;
  last: boolean;
  onRename: () => void;
  onItemSaved: () => void;
  onError: (message: string) => void;
};

export function BillRow({ bill, last, onRename, onItemSaved, onError }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className={!last ? "border-b border-[var(--color-border)]" : ""}>
      {/* Bill header */}
      <div
        onClick={() => setOpen((prev) => !prev)}
        className="group flex items-center gap-3 px-5 py-4 hover:bg-[var(--color-raised)] transition-colors cursor-pointer"
      >
        <svg
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
          className={`flex-shrink-0 text-[var(--color-muted)] transition-transform ${open ? "rotate-90" : ""}`}
        >
          <path
            d="M4 2L8 6L4 10"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        {/* Card icon */}
        <div className="w-7 h-7 rounded-md bg-[var(--color-raised)] border border-[var(--color-border)] flex items-center justify-center flex-shrink-0">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--color-secondary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="5" width="20" height="14" rx="2" />
            <path d="M2 10h20" />
          </svg>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-[var(--color-text)] text-sm font-medium truncate">
            {bill.name}
          </p>
          <span className="text-[var(--color-muted)] text-xs">
            {bill.items.length} {bill.items.length === 1 ? "compra" : "compras"}
            {bill.status === "OPEN" && (
              <span className="ml-2 text-[var(--color-teal)]">• Aberta</span>
            )}
          </span>
        </div>

        <span className="text-[var(--color-muted)] text-xs flex-shrink-0">
          {formatDay(bill.dueDate)}
        </span>

        <div className="flex items-center gap-2 flex-shrink-0 w-32 justify-end">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onRename();
            }}
            className="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded text-[var(--color-muted)] hover:text-[var(--color-teal)] hover:bg-[var(--color-border)] cursor-pointer"
            aria-label="Renomear fatura"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
          </button>
          <span className="text-sm font-semibold text-right text-[var(--color-expense)]">
            −{formatCurrency(bill.total)}
          </span>
        </div>
      </div>

      {/* Expanded purchases */}
      {open && (
        <div className="bg-[var(--color-bg)]">
          {bill.items.length === 0 && (
            <p className="pl-[4.25rem] pr-5 py-3 text-[var(--color-muted)] text-xs">
              Nenhuma compra nesta fatura.
            </p>
          )}
          {bill.items.map((item) => (
            <BillItemRow
              key={item.id}
              item={item}
              onSaved={onItemSaved}
              onError={onError}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function BillItemRow({
  item,
  onSaved,
  onError,
}: {
  item: Transaction;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const inline = useInlineDescription(item, onSaved, onError);
  const isIncome = item.type === "INCOME";
  const installment =
    item.totalInstallments && item.totalInstallments > 1
      ? `${item.installmentNumber}/${item.totalInstallments}`
      : null;

  return (
    <div className="group flex items-center gap-3 pl-[4.25rem] pr-5 py-3 border-t border-[var(--color-border)]">
      {inline.editing ? (
        <InlineDescriptionBar
          value={inline.value}
          onChange={inline.setValue}
          onSave={inline.save}
          onCancel={inline.cancel}
          saving={inline.saving}
        />
      ) : (
        <>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <p className="text-[var(--color-secondary)] text-sm truncate">
                {item.description}
              </p>
              {installment && (
                <span className="flex-shrink-0 text-[10px] font-medium text-[var(--color-muted)] bg-[var(--color-raised)] border border-[var(--color-border)] rounded px-1.5 py-0.5">
                  Parcela {installment}
                </span>
              )}
            </div>
            {item.category && (
              <span className="text-[var(--color-muted)] text-xs">
                {item.category.name}
              </span>
            )}
          </div>

          <span className="text-[var(--color-muted)] text-xs flex-shrink-0">
            {formatDay(item.date)}
          </span>

          <div className="flex items-center gap-2 flex-shrink-0 w-32 justify-end">
            <button
              onClick={inline.start}
              className="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded text-[var(--color-muted)] hover:text-[var(--color-teal)] hover:bg-[var(--color-border)] cursor-pointer"
              aria-label="Editar descrição"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 20h9" />
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
              </svg>
            </button>
            <span
              className="text-sm font-medium text-right"
              style={{
                color: isIncome
                  ? "var(--color-income)"
                  : "var(--color-secondary)",
              }}
            >
              {isIncome ? "+" : "−"}
              {formatCurrency(item.amount)}
            </span>
          </div>
        </>
      )}
    </div>
  );
}
