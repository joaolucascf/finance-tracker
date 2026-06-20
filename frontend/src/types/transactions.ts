export type Transaction = {
  id: number;
  amount: number;
  type: "INCOME" | "EXPENSE";
  description: string;
  date: string;
  category: {
    id: number;
    name: string;
  };
};

export type FormState = {
  description: string;
  amount: number | "";
  type: "EXPENSE" | "INCOME";
  categoryId: number | null;
  date: string;
};
