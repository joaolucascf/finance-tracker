import { getTransactions } from "@/services/transactions";
import { Transaction } from "@/types/transactions";
import { useEffect, useState } from "react";

export function useTransactions() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);

    try {
      const data = await getTransactions();
      if (!data) return;

      setTransactions(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao carregar transações",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return {
    transactions,
    loading,
    error,
    reload: load,
  };
}
