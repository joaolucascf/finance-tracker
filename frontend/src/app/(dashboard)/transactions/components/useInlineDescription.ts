import { useState } from "react";
import { Transaction } from "@/types/transactions";
import { updateTransactionDescription } from "@/services/transactions";

/** Inline "edit only the description" state machine, shared by table rows and bill items. */
export function useInlineDescription(
  transaction: Transaction,
  onSaved: () => void,
  onError?: (message: string) => void,
) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState(transaction.description ?? "");
  const [saving, setSaving] = useState(false);

  function start() {
    setValue(transaction.description ?? "");
    setEditing(true);
  }

  function cancel() {
    if (saving) return;
    setEditing(false);
  }

  async function save() {
    if (saving) return;
    setSaving(true);
    try {
      await updateTransactionDescription(transaction, value);
      setEditing(false);
      onSaved();
    } catch (err) {
      const message =
        err && typeof err === "object" && "message" in err
          ? String((err as { message: unknown }).message)
          : "Erro ao salvar";
      onError?.(message);
    } finally {
      setSaving(false);
    }
  }

  return { editing, value, setValue, saving, start, cancel, save };
}
