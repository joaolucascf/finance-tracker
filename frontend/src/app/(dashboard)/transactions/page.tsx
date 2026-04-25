"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/services/api";

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);

  useEffect(() => {
    async function load() {
      const res = await apiFetch("/transactions");

      if (!res) return; // caso tenha redirecionado

      const data = await res.json();
      setTransactions(data);
    }
    load();
  }, []);

  return (
    <div>
      <h1>Transações</h1>
      <pre>{JSON.stringify(transactions, null, 2)}</pre>
    </div>
  );
}