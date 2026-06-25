import { getTransactions } from "@/services/transactions";
import { LedgerEntry } from "@/types/transactions";
import { useCallback, useEffect, useState } from "react";

export function useTransactions(year: number, month: number) {
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await getTransactions(year, month);
      if (!data) return;

      setEntries(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao carregar transações",
      );
    } finally {
      setLoading(false);
    }
  }, [year, month]);

  useEffect(() => {
    load();
  }, [load]);

  return {
    entries,
    loading,
    error,
    reload: load,
  };
}
