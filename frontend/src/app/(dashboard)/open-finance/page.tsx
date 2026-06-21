"use client";

import dynamic from "next/dynamic";
import { useState } from "react";
import { useOpenFinance } from "@/hooks/useOpenFinance";
import { getConnectToken, registerConnection } from "@/services/openFinance";
import { ApiError } from "@/types/api-error";
import { AccountType, FinancialAccount, FinancialConnection } from "@/types/open-finance";

const PluggyConnect = dynamic(
  () => import("react-pluggy-connect").then((m) => ({ default: m.PluggyConnect })),
  { ssr: false }
);

function formatCurrency(value: number, currency = "BRL") {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(value);
}

const accountTypeLabel: Record<AccountType, string> = {
  CONTA_CORRENTE: "Conta Corrente",
  POUPANCA: "Poupança",
  CARTAO_CREDITO: "Cartão de Crédito",
};

const statusLabel: Record<string, string> = {
  ACTIVE: "Ativa",
  ERROR: "Erro",
  DISCONNECTED: "Desconectada",
};

const statusColor: Record<string, string> = {
  ACTIVE: "var(--color-income)",
  ERROR: "var(--color-expense)",
  DISCONNECTED: "var(--color-muted)",
};

export default function OpenFinancePage() {
  const { connections, accounts, loading, error, disconnect, sync, reload } = useOpenFinance();

  const [connectToken, setConnectToken] = useState<string | null>(null);
  const [connecting, setConnecting] = useState(false);
  const [connectError, setConnectError] = useState<string | null>(null);
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [disconnectingId, setDisconnectingId] = useState<number | null>(null);

  async function handleOpenWidget() {
    setConnecting(true);
    setConnectError(null);
    try {
      const { connectToken: token } = await getConnectToken();
      setConnectToken(token);
    } catch (err) {
      setConnectError((err as ApiError)?.message ?? "Erro ao iniciar conexão");
      setConnecting(false);
    }
  }

  async function handleSuccess({ item }: { item: { id: string; connector?: { name?: string } } }) {
    setConnectToken(null);
    setConnecting(false);
    const institutionName = item.connector?.name ?? "Instituição";
    try {
      await registerConnection({ itemId: item.id, institutionName });
      reload();
    } catch (err) {
      setConnectError((err as ApiError)?.message ?? "Erro ao registrar conexão");
    }
  }

  function handleClose() {
    setConnectToken(null);
    setConnecting(false);
  }

  async function handleSync(id: number) {
    setSyncingId(id);
    try {
      await sync(id);
      reload();
    } finally {
      setSyncingId(null);
    }
  }

  async function handleDisconnect(id: number) {
    setDisconnectingId(id);
    try {
      await disconnect(id);
    } finally {
      setDisconnectingId(null);
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">
          Open Finance
        </h1>
        <button
          onClick={handleOpenWidget}
          disabled={connecting}
          className="flex items-center gap-1.5 bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors cursor-pointer disabled:opacity-60"
        >
          <span className="text-base leading-none">+</span>
          Conectar banco
        </button>
      </div>

      {connectError && (
        <div
          className="rounded-xl border p-4 text-sm"
          style={{
            borderColor: "color-mix(in srgb, var(--color-expense) 30%, transparent)",
            backgroundColor: "color-mix(in srgb, var(--color-expense) 10%, transparent)",
            color: "var(--color-expense)",
          }}
        >
          {connectError}
        </div>
      )}

      {/* Pluggy widget */}
      {connectToken && (
        <PluggyConnect
          connectToken={connectToken}
          includeSandbox={true}
          onSuccess={handleSuccess as any}
          onError={({ message }) => {
            setConnectError(message ?? "Erro na conexão");
            handleClose();
          }}
          onClose={handleClose}
          theme="dark"
          language="pt"
        />
      )}

      {loading && (
        <div className="flex items-center justify-center py-20">
          <p className="text-[var(--color-muted)] text-sm">Carregando...</p>
        </div>
      )}

      {error && (
        <div
          className="rounded-xl border p-4 text-sm"
          style={{
            borderColor: "color-mix(in srgb, var(--color-expense) 30%, transparent)",
            backgroundColor: "color-mix(in srgb, var(--color-expense) 10%, transparent)",
            color: "var(--color-expense)",
          }}
        >
          {error}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && connections.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-14 h-14 rounded-full bg-[var(--color-surface)] border border-[var(--color-border)] flex items-center justify-center mb-4">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <rect x="2" y="5" width="20" height="14" rx="2" stroke="var(--color-muted)" strokeWidth="1.5" />
              <path d="M2 10h20" stroke="var(--color-muted)" strokeWidth="1.5" />
              <path d="M6 15h4" stroke="var(--color-muted)" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </div>
          <p className="text-[var(--color-secondary)] text-sm font-medium">
            Nenhuma conta conectada
          </p>
          <p className="text-[var(--color-muted)] text-xs mt-1">
            Conecte seu banco para importar transações automaticamente.
          </p>
        </div>
      )}

      {/* Connections */}
      {!loading && connections.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium">
            Instituições conectadas
          </h2>
          <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] overflow-hidden">
            {connections.map((conn, i) => (
              <ConnectionItem
                key={conn.id}
                connection={conn}
                last={i === connections.length - 1}
                syncing={syncingId === conn.id}
                disconnecting={disconnectingId === conn.id}
                onSync={() => handleSync(conn.id)}
                onDisconnect={() => handleDisconnect(conn.id)}
              />
            ))}
          </div>
        </section>
      )}

      {/* Accounts */}
      {!loading && accounts.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium">
            Contas importadas
          </h2>
          <div className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] overflow-hidden">
            {accounts.map((account, i) => (
              <AccountItem
                key={account.id}
                account={account}
                last={i === accounts.length - 1}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function ConnectionItem({
  connection: c,
  last,
  syncing,
  disconnecting,
  onSync,
  onDisconnect,
}: {
  connection: FinancialConnection;
  last: boolean;
  syncing: boolean;
  disconnecting: boolean;
  onSync: () => void;
  onDisconnect: () => void;
}) {
  return (
    <div
      className={`flex items-center gap-4 px-5 py-4 ${
        !last ? "border-b border-[var(--color-border)]" : ""
      }`}
    >
      <div className="flex-1 min-w-0">
        <p className="text-[var(--color-text)] text-sm font-medium truncate">
          {c.institutionName}
        </p>
        <div className="flex items-center gap-1.5 mt-0.5">
          <span
            className="w-1.5 h-1.5 rounded-full flex-shrink-0"
            style={{ backgroundColor: statusColor[c.status] }}
          />
          <span className="text-[var(--color-muted)] text-xs">
            {statusLabel[c.status] ?? c.status}
          </span>
        </div>
      </div>

      {c.status !== "DISCONNECTED" && (
        <button
          onClick={onSync}
          disabled={syncing}
          className="text-xs text-[var(--color-secondary)] hover:text-[var(--color-text)] px-3 py-1.5 rounded-lg border border-[var(--color-border)] hover:bg-[var(--color-raised)] transition-colors cursor-pointer disabled:opacity-50"
        >
          {syncing ? "Sincronizando..." : "Sincronizar"}
        </button>
      )}

      {c.status !== "DISCONNECTED" && (
        <button
          onClick={onDisconnect}
          disabled={disconnecting}
          className="text-xs text-[var(--color-expense)] hover:text-[var(--color-expense)] px-3 py-1.5 rounded-lg border border-[var(--color-border)] hover:bg-[var(--color-raised)] transition-colors cursor-pointer disabled:opacity-50"
        >
          {disconnecting ? "Desconectando..." : "Desconectar"}
        </button>
      )}
    </div>
  );
}

function AccountItem({
  account: a,
  last,
}: {
  account: FinancialAccount;
  last: boolean;
}) {
  const isCredit = a.type === "CARTAO_CREDITO";

  return (
    <div
      className={`flex items-center gap-4 px-5 py-4 ${
        !last ? "border-b border-[var(--color-border)]" : ""
      }`}
    >
      <div className="flex-1 min-w-0">
        <p className="text-[var(--color-text)] text-sm font-medium truncate">{a.name}</p>
        <span className="text-[var(--color-muted)] text-xs">
          {accountTypeLabel[a.type] ?? a.type}
        </span>
      </div>
      <span
        className="text-sm font-semibold flex-shrink-0"
        style={{ color: isCredit ? "var(--color-expense)" : "var(--color-income)" }}
      >
        {formatCurrency(a.currentBalance, a.currency)}
      </span>
    </div>
  );
}
