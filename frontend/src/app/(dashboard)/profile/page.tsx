"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useProfile } from "@/hooks/useProfile";
import { updateProfile, uploadPhoto } from "@/services/profile";
import { ProfileFormState, MARITAL_STATUS_OPTIONS, getPhotoSrc } from "@/types/profile";

const inputClass =
  "w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2.5 text-[var(--color-text)] text-sm placeholder:text-[var(--color-muted)] focus:outline-none focus:border-[var(--color-teal)] transition-colors disabled:opacity-50";

function getInitials(name: string) {
  return name
    .split(" ")
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase();
}

export default function ProfilePage() {
  const router = useRouter();
  const { profile, loading, setProfile } = useProfile();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState<ProfileFormState>({
    nickname: "",
    birthDate: "",
    monthlyIncome: "",
    maritalStatus: "",
  });
  const [saving, setSaving] = useState(false);
  const [pendingPhoto, setPendingPhoto] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const savedPhotoSrc = getPhotoSrc(profile);
  const displayPhotoSrc = previewUrl ?? savedPhotoSrc;

  useEffect(() => {
    if (!profile) return;
    setForm({
      nickname: profile.nickname ?? "",
      birthDate: profile.birthDate ?? "",
      monthlyIncome: profile.monthlyIncome?.toString() ?? "",
      maritalStatus: profile.maritalStatus ?? "",
    });
  }, [profile]);

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handlePhotoSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingPhoto(file);
    setPreviewUrl(URL.createObjectURL(file));
    e.target.value = "";
  }

  function handleCancel() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPendingPhoto(null);
    setPreviewUrl(null);
    router.push("/transactions");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      if (pendingPhoto) {
        const withPhoto = await uploadPhoto(pendingPhoto);
        setProfile(withPhoto);
      }
      const updated = await updateProfile(form);
      setProfile(updated);
      router.push("/transactions");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-[var(--color-muted)] text-sm">Carregando...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <h1 className="text-[var(--color-text)] text-2xl font-bold tracking-tight">Perfil</h1>

      {/* Avatar + info */}
      <div className="flex items-center gap-6">
        <div className="relative group flex-shrink-0">
          <div className="w-20 h-20 rounded-full overflow-hidden flex items-center justify-center bg-[var(--color-raised)] border border-[var(--color-border)]">
            {displayPhotoSrc ? (
              <img src={displayPhotoSrc} alt="Foto de perfil" className="w-full h-full object-cover" />
            ) : (
              <span className="text-2xl font-semibold text-[var(--color-secondary)]">
                {profile?.name ? getInitials(profile.name) : "—"}
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="absolute inset-0 rounded-full flex items-center justify-center bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
            aria-label="Alterar foto de perfil"
          >
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path
                d="M12.5 2.5a1.5 1.5 0 0 1 2.121 0l.879.879a1.5 1.5 0 0 1 0 2.121L6 15H3v-3L12.5 2.5Z"
                stroke="white"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handlePhotoSelect}
          />
        </div>

        <div>
          <p className="text-[var(--color-text)] font-semibold text-lg">{profile?.name}</p>
          <p className="text-[var(--color-muted)] text-sm">{profile?.email}</p>
          {pendingPhoto && (
            <p className="text-[var(--color-teal)] text-xs mt-1">Nova foto selecionada — salve para confirmar.</p>
          )}
        </div>
      </div>

      {/* Form */}
      <form
        onSubmit={handleSubmit}
        className="rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] p-6 space-y-5"
      >
        <div className="grid grid-cols-2 gap-5">
          <div className="space-y-1.5">
            <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
              Apelido
            </label>
            <input
              name="nickname"
              placeholder="Como prefere ser chamado"
              value={form.nickname}
              onChange={handleChange}
              className={inputClass}
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
              Data de Nascimento
            </label>
            <input
              name="birthDate"
              type="date"
              value={form.birthDate}
              onChange={handleChange}
              className={inputClass}
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
              Renda Mensal (R$)
            </label>
            <input
              name="monthlyIncome"
              type="number"
              min="0"
              step="0.01"
              placeholder="0,00"
              value={form.monthlyIncome}
              onChange={handleChange}
              className={inputClass}
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs text-[var(--color-secondary)] font-medium uppercase tracking-wide">
              Estado Civil
            </label>
            <select
              name="maritalStatus"
              value={form.maritalStatus}
              onChange={handleChange}
              className={inputClass + " cursor-pointer"}
            >
              <option value="">Não informado</option>
              {MARITAL_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={handleCancel}
            className="px-6 py-2.5 rounded-lg border border-[var(--color-border)] text-[var(--color-secondary)] hover:text-[var(--color-text)] hover:border-[var(--color-muted)] text-sm font-medium transition-colors cursor-pointer"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2.5 rounded-lg bg-[var(--color-teal)] hover:bg-[var(--color-teal-dark)] text-white font-semibold text-sm transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? "Salvando..." : "Salvar"}
          </button>
        </div>
      </form>
    </div>
  );
}
