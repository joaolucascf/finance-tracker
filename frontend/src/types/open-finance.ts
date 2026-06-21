export type ConnectionStatus = "ACTIVE" | "ERROR" | "DISCONNECTED";

export type AccountType = "CONTA_CORRENTE" | "POUPANCA" | "CARTAO_CREDITO";

export type FinancialConnection = {
  id: number;
  institutionName: string;
  provider: string;
  status: ConnectionStatus;
  createdAt: string;
  updatedAt: string;
};

export type FinancialAccount = {
  id: number;
  connectionId: number;
  name: string;
  type: AccountType;
  currentBalance: number;
  currency: string;
};
