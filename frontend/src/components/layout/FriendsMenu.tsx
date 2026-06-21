"use client";

import { useEffect, useRef, useState } from "react";
import { useFriends } from "@/hooks/useFriends";
import { sendFriendRequest } from "@/services/friends";
import { ApiError } from "@/types/api-error";
import { getPhotoSrc } from "@/types/profile";

function getInitials(name: string) {
  return name
    .split(" ")
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase();
}

function Avatar({
  name,
  photoBase64,
  photoType,
}: {
  name: string;
  photoBase64: string | null;
  photoType: string | null;
}) {
  const photoSrc = getPhotoSrc({ photoBase64, photoType });
  return (
    <div className="w-8 h-8 rounded-full overflow-hidden bg-[var(--color-raised)] border border-[var(--color-border)] flex items-center justify-center flex-shrink-0">
      {photoSrc ? (
        <img src={photoSrc} alt={name} className="w-full h-full object-cover" />
      ) : (
        <span className="text-[10px] font-semibold text-[var(--color-secondary)]">
          {getInitials(name)}
        </span>
      )}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-10 text-center">
      <div className="relative mb-4">
        <div className="w-16 h-16 rounded-full bg-[var(--color-raised)] border border-[var(--color-border)] flex items-center justify-center">
          <svg
            width="30"
            height="30"
            viewBox="0 0 24 24"
            fill="none"
            stroke="var(--color-muted)"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="12" cy="8" r="3.5" />
            <path d="M5.5 19a6.5 6.5 0 0 1 13 0" />
          </svg>
        </div>
        <span className="absolute -right-1 -bottom-1 w-6 h-6 rounded-full bg-[var(--color-teal)] text-white text-xs font-bold flex items-center justify-center border-2 border-[var(--color-surface)]">
          ?
        </span>
      </div>
      <p className="text-[var(--color-text)] text-sm font-medium">
        Nenhum amigo ainda
      </p>
      <p className="text-[var(--color-muted)] text-xs mt-1 max-w-[13rem]">
        Adicione amigos pelo e-mail para começar a compartilhar.
      </p>
    </div>
  );
}

export default function FriendsMenu() {
  const { friends, requests, loading, error, accept, reject } = useFriends();
  const [open, setOpen] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  useEffect(() => {
    if (!open) {
      setIsAdding(false);
      setEmail("");
      setFormError(null);
      setSuccess(null);
      setActionError(null);
    }
  }, [open]);

  useEffect(() => {
    if (!success) return;
    const timer = setTimeout(() => setSuccess(null), 4000);
    return () => clearTimeout(timer);
  }, [success]);

  function startAdding() {
    setIsAdding(true);
    setFormError(null);
    setSuccess(null);
    setTimeout(() => inputRef.current?.focus(), 0);
  }

  function cancelAdding() {
    setIsAdding(false);
    setEmail("");
    setFormError(null);
  }

  async function handleSubmit() {
    const value = email.trim();
    if (!value) {
      setFormError("Informe um e-mail");
      inputRef.current?.focus();
      return;
    }
    try {
      setSubmitting(true);
      setFormError(null);
      await sendFriendRequest(value);
      setSuccess(`Convite enviado para ${value}`);
      setEmail("");
      setIsAdding(false);
    } catch (err) {
      const apiError = err as ApiError;
      const emailError = apiError?.fields?.find(
        (f) => f.field === "email",
      )?.message;
      setFormError(emailError ?? apiError?.message ?? "Erro ao enviar convite");
    } finally {
      setSubmitting(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      handleSubmit();
    }
    if (e.key === "Escape") {
      e.stopPropagation();
      cancelAdding();
    }
  }

  async function handleAccept(id: number) {
    try {
      setProcessingId(id);
      setActionError(null);
      await accept(id);
    } catch (err) {
      setActionError((err as ApiError)?.message ?? "Erro ao aceitar convite");
    } finally {
      setProcessingId(null);
    }
  }

  async function handleReject(id: number) {
    try {
      setProcessingId(id);
      setActionError(null);
      await reject(id);
    } catch (err) {
      setActionError((err as ApiError)?.message ?? "Erro ao recusar convite");
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen((prev) => !prev)}
        className="w-9 h-9 rounded-full flex items-center justify-center bg-[var(--color-raised)] border border-[var(--color-border)] text-[var(--color-secondary)] hover:border-[var(--color-teal)] hover:text-[var(--color-text)] transition-colors cursor-pointer"
        aria-label="Amigos"
      >
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M16 19v-1a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v1" />
          <circle cx="9" cy="7" r="3" />
          <path d="M22 19v-1a4 4 0 0 0-3-3.87" />
          <path d="M16 4.13a4 4 0 0 1 0 7.75" />
        </svg>
      </button>

      {requests.length > 0 && (
        <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[var(--color-teal)] text-white text-[10px] font-bold flex items-center justify-center border-2 border-[var(--color-surface)]">
          {requests.length}
        </span>
      )}

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-xl overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--color-border)]">
            <p className="text-[var(--color-text)] text-sm font-semibold">
              Amigos
            </p>
            {friends.length > 0 && (
              <span className="text-[var(--color-muted)] text-xs">
                {friends.length}
              </span>
            )}
          </div>

          <div className="max-h-72 overflow-y-auto">
            {loading ? (
              <div className="px-4 py-10 text-center text-[var(--color-muted)] text-sm">
                Carregando…
              </div>
            ) : error ? (
              <div className="px-4 py-10 text-center text-[var(--color-expense)] text-sm">
                {error}
              </div>
            ) : (
              <>
                {requests.length > 0 && (
                  <div className="border-b border-[var(--color-border)]">
                    <p className="px-4 pt-3 pb-1.5 text-[var(--color-muted)] text-[11px] font-semibold uppercase tracking-wide">
                      Convites recebidos
                    </p>
                    {actionError && (
                      <p className="px-4 pb-2 text-[var(--color-expense)] text-xs">
                        {actionError}
                      </p>
                    )}
                    <ul className="pb-1">
                      {requests.map((req) => (
                        <li
                          key={req.id}
                          className="flex items-center gap-3 px-4 py-2"
                        >
                          <Avatar
                            name={req.name}
                            photoBase64={req.photoBase64}
                            photoType={req.photoType}
                          />
                          <div className="min-w-0 flex-1">
                            <p className="text-[var(--color-text)] text-sm truncate">
                              {req.name}
                            </p>
                            <p className="text-[var(--color-muted)] text-xs truncate">
                              {req.email}
                            </p>
                          </div>
                          <div className="flex items-center gap-1.5 flex-shrink-0">
                            <button
                              onClick={() => handleAccept(req.id)}
                              disabled={processingId === req.id}
                              aria-label="Aceitar convite"
                              className="w-7 h-7 rounded-lg bg-[var(--color-teal)] text-white flex items-center justify-center hover:bg-[var(--color-teal-dark)] disabled:opacity-50 transition-colors cursor-pointer"
                            >
                              <svg
                                width="14"
                                height="14"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                              >
                                <path d="M20 6L9 17l-5-5" />
                              </svg>
                            </button>
                            <button
                              onClick={() => handleReject(req.id)}
                              disabled={processingId === req.id}
                              aria-label="Recusar convite"
                              className="w-7 h-7 rounded-lg border border-[var(--color-border)] text-[var(--color-secondary)] flex items-center justify-center hover:border-[var(--color-expense)] hover:text-[var(--color-expense)] disabled:opacity-50 transition-colors cursor-pointer"
                            >
                              <svg
                                width="12"
                                height="12"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                              >
                                <path d="M18 6L6 18M6 6l12 12" />
                              </svg>
                            </button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {friends.length === 0 ? (
                  requests.length === 0 ? (
                    <EmptyState />
                  ) : (
                    <p className="px-4 py-6 text-center text-[var(--color-muted)] text-xs">
                      Você ainda não tem amigos. Aceite um convite para começar.
                    </p>
                  )
                ) : (
                  <ul className="py-1">
                    {friends.map((friend) => (
                      <li
                        key={friend.id}
                        className="flex items-center gap-3 px-4 py-2.5 hover:bg-[var(--color-raised)] transition-colors"
                      >
                        <Avatar
                          name={friend.name}
                          photoBase64={friend.photoBase64}
                          photoType={friend.photoType}
                        />
                        <div className="min-w-0">
                          <p className="text-[var(--color-text)] text-sm truncate">
                            {friend.name}
                          </p>
                          <p className="text-[var(--color-muted)] text-xs truncate">
                            {friend.email}
                          </p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </>
            )}
          </div>

          {success && (
            <div className="px-4 py-2.5 border-t border-[var(--color-border)] bg-[var(--color-raised)]">
              <p className="text-[var(--color-income)] text-xs flex items-center gap-1.5">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="flex-shrink-0"
                >
                  <path d="M20 6L9 17l-5-5" />
                </svg>
                {success}
              </p>
            </div>
          )}

          <div className="border-t border-[var(--color-border)] p-3">
            {isAdding ? (
              <div className="space-y-2">
                <input
                  ref={inputRef}
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setFormError(null);
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="E-mail do amigo"
                  disabled={submitting}
                  className="w-full bg-[var(--color-raised)] border border-[var(--color-teal)] rounded-lg px-3 py-2 text-sm text-[var(--color-text)] placeholder:text-[var(--color-muted)] focus:outline-none disabled:opacity-50"
                />
                {formError && (
                  <span className="text-[var(--color-expense)] text-xs block">
                    {formError}
                  </span>
                )}
                <div className="flex gap-2">
                  <button
                    onClick={handleSubmit}
                    disabled={submitting}
                    className="flex-1 px-3 py-2 rounded-lg bg-[var(--color-teal)] text-white text-sm font-medium hover:bg-[var(--color-teal-dark)] disabled:opacity-50 transition-colors cursor-pointer"
                  >
                    {submitting ? "Enviando…" : "Enviar convite"}
                  </button>
                  <button
                    onClick={cancelAdding}
                    disabled={submitting}
                    className="px-3 py-2 rounded-lg border border-[var(--color-border)] text-[var(--color-secondary)] text-sm hover:border-[var(--color-teal)] transition-colors cursor-pointer disabled:opacity-50"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={startAdding}
                className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-[var(--color-teal)] text-white text-sm font-medium hover:bg-[var(--color-teal-dark)] transition-colors cursor-pointer"
              >
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M12 5v14M5 12h14" />
                </svg>
                Adicionar amigo
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
