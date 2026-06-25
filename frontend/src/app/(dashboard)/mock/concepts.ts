export type RefSource = "connections" | "accounts" | "bills" | "categories";

export type MockField = {
  name: string;
  label: string;
  type: "text" | "number" | "date" | "select" | "boolean" | "ref";
  options?: string[];
  /** for type "ref": which existing records populate the dropdown */
  ref?: RefSource;
  /** for type "ref": only show records whose `key` equals this value */
  refFilter?: { key: string; equals: string };
  placeholder?: string;
};

export type MockConcept = {
  /** route param + backend path segment (/mock/{path}) */
  path: string;
  label: string;
  hint: string;
  fields: MockField[];
};

/** Builds a human label for a record used as a dropdown option. */
export const REF_LABEL: Record<RefSource, (r: Record<string, unknown>) => string> = {
  connections: (r) =>
    `#${String(r.id)} · ${String(r.institutionName)} (${String(r.provider)})`,
  accounts: (r) => `#${String(r.id)} · ${String(r.name)} [${String(r.type)}]`,
  bills: (r) =>
    `#${String(r.id)} · ${
      r.customName ? String(r.customName) : "Fatura #" + String(r.sequence)
    } · vence ${String(r.dueDate)}`,
  categories: (r) => `#${String(r.id)} · ${String(r.name)}`,
};

export const CONCEPTS: Record<string, MockConcept> = {
  connections: {
    path: "connections",
    label: "Conexões",
    hint: "Instituição conectada (pré-requisito de uma conta).",
    fields: [
      { name: "institutionName", label: "Instituição", type: "text", placeholder: "Banco Mock" },
      { name: "provider", label: "Provider", type: "text", placeholder: "PLUGGY" },
      {
        name: "status",
        label: "Status",
        type: "select",
        options: ["ACTIVE", "ERROR", "DISCONNECTED"],
      },
    ],
  },
  accounts: {
    path: "accounts",
    label: "Contas",
    hint: "Conta de uma conexão. Para faturas, use o tipo CARTAO_CREDITO.",
    fields: [
      { name: "connectionId", label: "Conexão", type: "ref", ref: "connections" },
      { name: "name", label: "Nome", type: "text", placeholder: "Cartão Mock" },
      {
        name: "type",
        label: "Tipo",
        type: "select",
        options: ["CARTAO_CREDITO", "CONTA_CORRENTE", "POUPANCA"],
      },
      { name: "currentBalance", label: "Saldo", type: "number" },
      { name: "currency", label: "Moeda", type: "text", placeholder: "BRL" },
    ],
  },
  bills: {
    path: "bills",
    label: "Faturas",
    hint: "Fatura de uma conta de cartão. A ABERTA é a de maior vencimento.",
    fields: [
      {
        name: "accountId",
        label: "Conta (cartão)",
        type: "ref",
        ref: "accounts",
        refFilter: { key: "type", equals: "CARTAO_CREDITO" },
      },
      { name: "dueDate", label: "Vencimento (data de competência)", type: "date" },
      { name: "totalAmount", label: "Total (R$)", type: "number" },
      { name: "status", label: "Status", type: "select", options: ["OPEN", "CLOSED"] },
      { name: "sequence", label: "Sequência (#N)", type: "number" },
      { name: "customName", label: "Nome customizado", type: "text" },
    ],
  },
  transactions: {
    path: "transactions",
    label: "Transações",
    hint: "Compra/lançamento. Vincule a uma fatura e use parcelas para o badge.",
    fields: [
      { name: "description", label: "Descrição", type: "text" },
      { name: "amount", label: "Valor (R$)", type: "number" },
      { name: "type", label: "Tipo", type: "select", options: ["EXPENSE", "INCOME"] },
      { name: "date", label: "Data", type: "date" },
      { name: "imported", label: "Importada?", type: "boolean" },
      { name: "sourceAccountId", label: "Conta de origem", type: "ref", ref: "accounts" },
      { name: "billId", label: "Fatura", type: "ref", ref: "bills" },
      { name: "categoryId", label: "Categoria", type: "ref", ref: "categories" },
      { name: "installmentNumber", label: "Parcela nº", type: "number" },
      { name: "totalInstallments", label: "Total de parcelas", type: "number" },
    ],
  },
};
