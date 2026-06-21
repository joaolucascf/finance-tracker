import { useEffect, useState } from "react";
import {
  disconnectConnection,
  getAccounts,
  getConnections,
  syncConnection,
} from "@/services/openFinance";
import { ApiError } from "@/types/api-error";
import { FinancialAccount, FinancialConnection } from "@/types/open-finance";

export function useOpenFinance() {
  const [connections, setConnections] = useState<FinancialConnection[]>([]);
  const [accounts, setAccounts] = useState<FinancialAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [conns, accs] = await Promise.all([getConnections(), getAccounts()]);
      setConnections(conns ?? []);
      setAccounts(accs ?? []);
    } catch (err) {
      setError((err as ApiError)?.message ?? "Erro ao carregar dados");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function disconnect(id: number) {
    await disconnectConnection(id);
    setConnections((prev) =>
      prev.map((c) => (c.id === id ? { ...c, status: "DISCONNECTED" as const } : c))
    );
  }

  async function sync(id: number) {
    await syncConnection(id);
  }

  return { connections, accounts, loading, error, disconnect, sync, reload: load };
}
