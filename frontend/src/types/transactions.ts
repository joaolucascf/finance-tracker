export type Transaction = {
  id: number;
  amount: number;
  type: "INCOME" | "EXPENSE";
  description: string;
  date: string;
  category: {
    id: number;
    name: string;
  } | null;
  imported: boolean;
  installmentNumber?: number | null;
  totalInstallments?: number | null;
};

export type Bill = {
  id: number;
  name: string;
  total: number;
  dueDate: string;
  status: "OPEN" | "CLOSED";
  items: Transaction[];
};

export type LedgerEntry = {
  type: "TRANSACTION" | "BILL";
  date: string;
  transaction: Transaction | null;
  bill: Bill | null;
};

export type FormState = {
  description: string;
  amount: number;
  type: "EXPENSE" | "INCOME";
  categoryId: number | null;
  date: string;
};
