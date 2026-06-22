import { getTransactions } from "@/services/transactions";
import { Transaction } from "@/types/transactions";
import { useCallback, useEffect, useState } from "react";

export function useTransactions(year: number, month: number) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await getTransactions(year, month);
      if (!data) return;

      setTransactions(data);
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
    transactions,
    loading,
    error,
    reload: load,
  };
}
