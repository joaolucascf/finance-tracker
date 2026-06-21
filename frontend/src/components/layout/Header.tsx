"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { logout } from "@/lib/auth";
import { useProfile } from "@/hooks/useProfile";
import { getPhotoSrc } from "@/types/profile";
import FriendsMenu from "@/components/layout/FriendsMenu";

function getInitials(name: string) {
  return name
    .split(" ")
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase();
}

export default function Header() {
  const { profile } = useProfile();
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const photoSrc = getPhotoSrc(profile);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className="sticky top-0 z-10 flex items-center justify-between px-8 py-5 border-b border-[var(--color-border)] bg-[var(--color-surface)]">
      <Link href="/transactions" className="flex items-center gap-2.5">
        <div className="w-7 h-7 rounded-lg bg-[var(--color-teal)] flex items-center justify-center">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M2 10L7 4L12 10" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </div>
        <span className="text-[var(--color-text)] font-semibold text-base tracking-tight">
          Finance
        </span>
      </Link>

      <div className="flex items-center gap-3">
        <FriendsMenu />

        <div className="relative" ref={dropdownRef}>
        <button
          onClick={() => setOpen((prev) => !prev)}
          className="w-9 h-9 rounded-full overflow-hidden flex items-center justify-center bg-[var(--color-raised)] border border-[var(--color-border)] hover:border-[var(--color-teal)] transition-colors cursor-pointer"
          aria-label="Menu do usuário"
        >
          {photoSrc ? (
            <img src={photoSrc} alt={profile?.name} className="w-full h-full object-cover" />
          ) : (
            <span className="text-xs font-semibold text-[var(--color-secondary)]">
              {profile?.name ? getInitials(profile.name) : "—"}
            </span>
          )}
        </button>

        {open && (
          <div className="absolute right-0 top-full mt-2 w-48 rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-xl overflow-hidden">
            {profile && (
              <div className="px-4 py-3 border-b border-[var(--color-border)]">
                <p className="text-[var(--color-text)] text-xs font-medium truncate">
                  {profile.nickname ?? profile.name}
                </p>
                <p className="text-[var(--color-muted)] text-xs truncate">{profile.email}</p>
              </div>
            )}
            <div className="py-1">
              <Link
                href="/profile"
                onClick={() => setOpen(false)}
                className="block px-4 py-2.5 text-sm text-[var(--color-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-raised)] transition-colors"
              >
                Perfil
              </Link>
              <Link
                href="/preferences"
                onClick={() => setOpen(false)}
                className="block px-4 py-2.5 text-sm text-[var(--color-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-raised)] transition-colors"
              >
                Preferências
              </Link>
              <button
                onClick={logout}
                className="w-full text-left px-4 py-2.5 text-sm text-[var(--color-expense)] hover:bg-[var(--color-raised)] transition-colors cursor-pointer"
              >
                Sair
              </button>
            </div>
          </div>
        )}
        </div>
      </div>
    </header>
  );
}
