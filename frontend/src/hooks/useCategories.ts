import { getCategories } from "@/services/categories";
import { Category } from "@/types/categories";
import { useEffect, useState } from "react";

export function useCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);

    try {
      const data = await getCategories();

      if (!data) {
        setCategories([]);
        return;
      }

      setCategories(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao carregar categorias",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return {
    categories,
    loading,
    error,
    reload: load,
  };
}
