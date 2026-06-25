"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { CONCEPTS, MockField, REF_LABEL, RefSource } from "../concepts";
import {
  createMock,
  deleteMock,
  listMock,
  updateMock,
} from "@/services/mock";
import { getCategories } from "@/services/categories";

type Row = Record<string, unknown> & { id: number };

const inputClass =
  "w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2 text-[var(--color-text)] text-sm focus:outline-none focus:border-[var(--color-teal)] transition-colors";

function buildPayload(fields: MockField[], values: Record<string, string>) {
  const payload: Record<string, unknown> = {};
  for (const f of fields) {
    const v = values[f.name];
    if (f.type === "boolean") {
      payload[f.name] = v === "true";
    } else if (v === undefined || v === "") {
      payload[f.name] = undefined;
    } else if (f.type === "number" || f.type === "ref") {
      payload[f.name] = Number(v);
    } else {
      payload[f.name] = v;
    }
  }
  return payload;
}

function emptyValues(fields: MockField[]) {
  const v: Record<string, string> = {};
  for (const f of fields) v[f.name] = f.type === "boolean" ? "false" : "";
  return v;
}

export default function MockConceptPage() {
  const { concept } = useParams<{ concept: string }>();
  const config = CONCEPTS[concept];

  const [rows, setRows] = useState<Row[]>([]);
  const [refOptions, setRefOptions] = useState<Record<string, Row[]>>({});
  const [values, setValues] = useState<Record<string, string>>({});
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!config) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listMock(config.path);
      setRows(data ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao carregar");
    } finally {
      setLoading(false);
    }
  }, [config]);

  const loadRefs = useCallback(async () => {
    if (!config) return;
    const sources = Array.from(
      new Set(
        config.fields
          .filter((f) => f.type === "ref" && f.ref)
          .map((f) => f.ref as RefSource),
      ),
    );
    const result: Record<string, Row[]> = {};
    await Promise.all(
      sources.map(async (src) => {
        try {
          const data = src === "categories" ? await getCategories() : await listMock(src);
          result[src] = data ?? [];
        } catch {
          result[src] = [];
        }
      }),
    );
    setRefOptions(result);
  }, [config]);

  useEffect(() => {
    if (config) setValues(emptyValues(config.fields));
    setEditingId(null);
    load();
    loadRefs();
  }, [config, load, loadRefs]);

  if (!config) {
    return (
      <div className="space-y-3">
        <p className="text-[var(--color-text)] text-sm">
          Conceito desconhecido: {concept}
        </p>
        <Link href="/mock" className="text-[var(--color-teal)] text-sm">
          ← voltar
        </Link>
      </div>
    );
  }

  function startEdit(row: Row) {
    const next: Record<string, string> = {};
    for (const f of config.fields) {
      const raw = row[f.name];
      next[f.name] =
        raw === null || raw === undefined ? (f.type === "boolean" ? "false" : "") : String(raw);
    }
    setValues(next);
    setEditingId(row.id);
  }

  function resetForm() {
    setValues(emptyValues(config.fields));
    setEditingId(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (saving) return;
    setSaving(true);
    setError(null);
    try {
      const payload = buildPayload(config.fields, values);
      if (editingId != null) {
        await updateMock(config.path, editingId, payload);
      } else {
        await createMock(config.path, payload);
      }
      resetForm();
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao salvar");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm("Excluir este registro?")) return;
    try {
      await deleteMock(config.path, id);
      if (editingId === id) resetForm();
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao excluir");
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">
            Mock · {config.label}
          </h1>
          <p className="text-[var(--color-muted)] text-xs mt-1">{config.hint}</p>
        </div>
        <Link
          href="/mock"
          className="text-[var(--color-secondary)] hover:text-[var(--color-text)] text-sm"
        >
          ← conceitos
        </Link>
      </div>

      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 space-y-3"
      >
        <p className="text-[var(--color-secondary)] text-xs uppercase tracking-wide font-medium">
          {editingId != null ? `Editar #${editingId}` : "Novo registro"}
        </p>
        <div className="grid grid-cols-2 gap-3">
          {config.fields.map((f) => (
            <label key={f.name} className="block space-y-1">
              <span className="text-[var(--color-muted)] text-xs">{f.label}</span>
              {f.type === "ref" && f.ref ? (
                <select
                  value={values[f.name] ?? ""}
                  onChange={(e) =>
                    setValues((p) => ({ ...p, [f.name]: e.target.value }))
                  }
                  className={inputClass}
                >
                  <option value="">—</option>
                  {(refOptions[f.ref] ?? [])
                    .filter(
                      (r) =>
                        !f.refFilter || r[f.refFilter.key] === f.refFilter.equals,
                    )
                    .map((r) => (
                      <option key={r.id} value={String(r.id)}>
                        {REF_LABEL[f.ref as RefSource](r)}
                      </option>
                    ))}
                </select>
              ) : f.type === "select" ? (
                <select
                  value={values[f.name] ?? ""}
                  onChange={(e) =>
                    setValues((p) => ({ ...p, [f.name]: e.target.value }))
                  }
                  className={inputClass}
                >
                  <option value="">—</option>
                  {f.options?.map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              ) : f.type === "boolean" ? (
                <select
                  value={values[f.name] ?? "false"}
                  onChange={(e) =>
                    setValues((p) => ({ ...p, [f.name]: e.target.value }))
                  }
                  className={inputClass}
                >
                  <option value="true">Sim</option>
                  <option value="false">Não</option>
                </select>
              ) : (
                <input
                  type={f.type === "number" ? "number" : f.type === "date" ? "date" : "text"}
                  step={f.type === "number" ? "any" : undefined}
                  placeholder={f.placeholder}
                  value={values[f.name] ?? ""}
                  onChange={(e) =>
                    setValues((p) => ({ ...p, [f.name]: e.target.value }))
                  }
                  className={inputClass}
                />
              )}
            </label>
          ))}
        </div>
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={saving}
            className="bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
          >
            {saving ? "Salvando..." : editingId != null ? "Salvar" : "Criar"}
          </button>
          {editingId != null && (
            <button
              type="button"
              onClick={resetForm}
              className="text-[var(--color-secondary)] hover:text-[var(--color-text)] text-sm px-3 py-2 rounded-lg cursor-pointer"
            >
              Cancelar
            </button>
          )}
        </div>
      </form>

      {error && (
        <p className="text-[var(--color-expense)] text-sm">{error}</p>
      )}

      {/* Table */}
      {loading ? (
        <p className="text-[var(--color-muted)] text-sm">Carregando...</p>
      ) : rows.length === 0 ? (
        <p className="text-[var(--color-muted)] text-sm">Nenhum registro.</p>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-[var(--color-border)]">
          <table className="w-full text-xs">
            <thead>
              <tr className="bg-[var(--color-raised)] text-[var(--color-secondary)]">
                {Object.keys(rows[0]).map((k) => (
                  <th key={k} className="text-left font-medium px-3 py-2 whitespace-nowrap">
                    {k}
                  </th>
                ))}
                <th className="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.id}
                  className="border-t border-[var(--color-border)] text-[var(--color-text)]"
                >
                  {Object.keys(rows[0]).map((k) => (
                    <td key={k} className="px-3 py-2 whitespace-nowrap">
                      {row[k] === null || row[k] === undefined ? "—" : String(row[k])}
                    </td>
                  ))}
                  <td className="px-3 py-2 whitespace-nowrap text-right">
                    <button
                      onClick={() => startEdit(row)}
                      className="text-[var(--color-teal)] hover:underline mr-3 cursor-pointer"
                    >
                      editar
                    </button>
                    <button
                      onClick={() => handleDelete(row.id)}
                      className="text-[var(--color-expense)] hover:underline cursor-pointer"
                    >
                      excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
