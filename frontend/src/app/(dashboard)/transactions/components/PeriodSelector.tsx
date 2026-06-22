"use client";

import { useEffect, useLayoutEffect, useRef, useState } from "react";

type Props = {
  year: number;
  month: number; // 1-12
  onChange: (year: number, month: number) => void;
};

const MONTHS_FULL = [
  "Janeiro",
  "Fevereiro",
  "Março",
  "Abril",
  "Maio",
  "Junho",
  "Julho",
  "Agosto",
  "Setembro",
  "Outubro",
  "Novembro",
  "Dezembro",
];

const MONTHS_SHORT = [
  "Jan",
  "Fev",
  "Mar",
  "Abr",
  "Mai",
  "Jun",
  "Jul",
  "Ago",
  "Set",
  "Out",
  "Nov",
  "Dez",
];

const MIN_YEAR = 1970;
const MAX_YEAR = 9999;

/** Steps a month with wraparound; returns null when it would push the year out of bounds. */
function stepMonth(
  year: number,
  month: number,
  dir: number,
): { year: number; month: number } | null {
  let m = month + dir;
  let y = year;
  if (m < 1) {
    m = 12;
    y -= 1;
  } else if (m > 12) {
    m = 1;
    y += 1;
  }
  if (y < MIN_YEAR || y > MAX_YEAR) return null;
  return { year: y, month: m };
}

export function PeriodSelector({ year, month, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
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

  const yearCanPrev = year > MIN_YEAR;
  const yearCanNext = year < MAX_YEAR;

  const monthPrev = stepMonth(year, month, -1);
  const monthNext = stepMonth(year, month, 1);

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className={`flex items-center gap-2 bg-[var(--color-surface)] border rounded-lg px-3 py-2 text-sm font-medium text-[var(--color-text)] transition-colors cursor-pointer ${
          open
            ? "border-[var(--color-teal)]"
            : "border-[var(--color-border)] hover:border-[var(--color-muted)]"
        }`}
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
          className="text-[var(--color-muted)]"
        >
          <rect x="3" y="4" width="18" height="18" rx="2" />
          <path d="M16 2v4M8 2v4M3 10h18" />
        </svg>
        {MONTHS_SHORT[month - 1]} {year}
        <svg
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
          className={`text-[var(--color-muted)] transition-transform ${open ? "rotate-180" : ""}`}
        >
          <path
            d="M2 4L6 8L10 4"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 z-30 w-60 rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-xl p-4 space-y-3">
          <Carousel
            caption="Ano"
            currentLabel={String(year)}
            prevLabel={yearCanPrev ? String(year - 1) : ""}
            nextLabel={yearCanNext ? String(year + 1) : ""}
            canPrev={yearCanPrev}
            canNext={yearCanNext}
            onStep={(dir) => onChange(year + dir, month)}
          />
          <div className="h-px bg-[var(--color-border)]" />
          <Carousel
            caption="Mês"
            currentLabel={MONTHS_FULL[month - 1]}
            prevLabel={monthPrev ? MONTHS_FULL[monthPrev.month - 1] : ""}
            nextLabel={monthNext ? MONTHS_FULL[monthNext.month - 1] : ""}
            canPrev={!!monthPrev}
            canNext={!!monthNext}
            onStep={(dir) => {
              const next = stepMonth(year, month, dir);
              if (next) onChange(next.year, next.month);
            }}
          />
        </div>
      )}
    </div>
  );
}

type CarouselProps = {
  caption: string;
  currentLabel: string;
  prevLabel: string;
  nextLabel: string;
  canPrev: boolean;
  canNext: boolean;
  /** dir = +1 advances (future / next month), dir = -1 goes back. */
  onStep: (dir: number) => void;
};

const THRESHOLD = 36;
const ANIM_MS = 170;

function Carousel({
  caption,
  currentLabel,
  prevLabel,
  nextLabel,
  canPrev,
  canNext,
  onStep,
}: CarouselProps) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);
  const [dx, setDx] = useState(0);
  const [animating, setAnimating] = useState(false);
  const drag = useRef({ startX: 0, active: false, dx: 0 });
  const committing = useRef(false);

  useLayoutEffect(() => {
    if (viewportRef.current) setWidth(viewportRef.current.offsetWidth);
  }, []);

  function finishStep(dir: number) {
    if (committing.current || width === 0) {
      if (width === 0) onStep(dir);
      return;
    }
    committing.current = true;
    setAnimating(true);
    // Slide so the incoming neighbour lands in the centre, then snap back with new labels.
    setDx(dir > 0 ? -width : width);
    window.setTimeout(() => {
      setAnimating(false);
      setDx(0);
      onStep(dir);
      committing.current = false;
    }, ANIM_MS);
  }

  function snapBack() {
    setAnimating(true);
    setDx(0);
    window.setTimeout(() => setAnimating(false), ANIM_MS);
  }

  function onPointerDown(e: React.PointerEvent) {
    if (committing.current) return;
    e.currentTarget.setPointerCapture?.(e.pointerId);
    drag.current = { startX: e.clientX, active: true, dx: 0 };
    setAnimating(false);
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!drag.current.active) return;
    let delta = e.clientX - drag.current.startX;
    // Add resistance when there's nowhere to go in that direction.
    if (delta < 0 && !canNext) delta *= 0.25;
    if (delta > 0 && !canPrev) delta *= 0.25;
    drag.current.dx = delta;
    setDx(delta);
  }

  function endDrag() {
    if (!drag.current.active) return;
    drag.current.active = false;
    const delta = drag.current.dx;
    if (delta <= -THRESHOLD && canNext) finishStep(1);
    else if (delta >= THRESHOLD && canPrev) finishStep(-1);
    else snapBack();
  }

  const cellStyle = { width: width || undefined };

  return (
    <div className="space-y-1">
      <p className="text-center text-[10px] uppercase tracking-wide text-[var(--color-muted)] font-medium">
        {caption}
      </p>
      <div className="flex items-center gap-1">
        <Arrow
          direction="left"
          disabled={!canPrev}
          onClick={() => canPrev && finishStep(-1)}
        />
        <div
          ref={viewportRef}
          className="relative flex-1 overflow-hidden select-none touch-none cursor-grab active:cursor-grabbing"
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={endDrag}
          onPointerCancel={endDrag}
        >
          <div
            className="flex"
            style={{
              transform: `translateX(${-width + dx}px)`,
              transition: animating ? `transform ${ANIM_MS}ms ease-out` : "none",
            }}
          >
            <div
              className="flex-none text-center text-sm font-semibold text-[var(--color-secondary)]"
              style={cellStyle}
            >
              {prevLabel}
            </div>
            <div
              className="flex-none text-center text-sm font-semibold text-[var(--color-text)]"
              style={cellStyle}
            >
              {currentLabel}
            </div>
            <div
              className="flex-none text-center text-sm font-semibold text-[var(--color-secondary)]"
              style={cellStyle}
            >
              {nextLabel}
            </div>
          </div>
        </div>
        <Arrow
          direction="right"
          disabled={!canNext}
          onClick={() => canNext && finishStep(1)}
        />
      </div>
    </div>
  );
}

function Arrow({
  direction,
  disabled,
  onClick,
}: {
  direction: "left" | "right";
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      aria-label={direction === "left" ? "Anterior" : "Próximo"}
      className="flex-none w-7 h-7 flex items-center justify-center rounded-md text-[var(--color-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-raised)] transition-colors cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent"
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
        className={direction === "left" ? "" : "rotate-180"}
      >
        <path d="M15 18l-6-6 6-6" />
      </svg>
    </button>
  );
}
