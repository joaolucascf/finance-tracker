# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Personal finance tracker with a Spring Boot backend and Next.js frontend backed by PostgreSQL.

## Open threads / next steps

The implemented code + this file are the source of truth. The items below are **new requirements or bug fixes still open** — not descriptions of current behavior:

- **Card refunds/estornos** — card INCOME with null billId is currently all hidden (it's treated as a bill payment). A real estorno should instead *reduce* the open cycle. Deferred until there's real data; full decision notes in `requisito-estornos.txt`.
- **Mock CRUD must be gated/removed before any real deploy** — `MockController` + `/mock` are dev-only and currently unguarded.
- **Transition bill on a brand-new card has no `due_date`** (no closed bill to reference yet), so it won't show in the period filter until the first statement closes. Decide whether to estimate a placeholder due date.

## Running locally

The easiest way to start everything (DB + backend + frontend) is:
```bash
./run-local.sh
```

This script starts the PostgreSQL container, exports `.env` variables, and runs both servers concurrently. Requires SDKMAN and Docker.

**Individually:**
```bash
# Database only (required before running backend manually)
docker compose up -d db

# Backend (from /backend, requires env vars exported from .env)
export $(grep -v '^#' ../.env | xargs)
mvn spring-boot:run

# Frontend (from /frontend)
npm run dev
```

**Full stack via Docker:**
```bash
docker compose up
```

URLs: frontend `http://localhost:3000`, backend `http://localhost:8080`.

## Environment variables

The backend requires a `.env` file at the project root with:
```
DB_URL=jdbc:postgresql://localhost:5432/finance_tracker
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=<min 32 characters>
PLUGGY_CLIENT_ID=<Pluggy client id>
PLUGGY_CLIENT_SECRET=<Pluggy client secret>
```

## Backend commands (run from `/backend`)

```bash
mvn test                              # all tests
mvn test -Dtest=TransactionServiceTest   # single test class
mvn test -Dtest=TransactionIntegrationTest#methodName  # single method
mvn spring-boot:run                   # run with default profile
```

Integration tests require Docker — they spin up a real PostgreSQL 16 container via Testcontainers (`AbstractIntegrationTest` base class). Unit tests (`TransactionServiceTest`, `TransactionControllerTest`) use Mockito/MockMvc and don't need Docker.

## Frontend commands (run from `/frontend`)

```bash
npm run dev        # dev server
npm run build      # production build
npm run lint       # ESLint
npx tsc --noEmit   # type check
```

## Backend architecture

Layered Spring Boot app under `com.joaolucas.finance_tracker`:

- **config/** — `SecurityConfiguration`, `CorsConfig` (allows only `http://localhost:3000`), `SecurityBeans`, `OpenFinanceConfig` (`@EnableScheduling` + `@EnableConfigurationProperties`)
- **controller/** — REST endpoints (`AuthController`, `TransactionController`, `CategoryController`, `ProfileController`, `FriendshipController`, `OpenFinanceController`, `BillController`, `MockController` (dev-only, see "Mock CRUD"))
- **service/** — business logic; receives DTOs, operates on entities
- **repository/** — JPA repositories (Spring Data)
- **entity/** — JPA entities (`User`, `UserProfile`, `RefreshToken`, `Transaction`, `Category`, `TransactionType` enum, `Friendship`, `FriendshipStatus` enum, `FinancialConnection`, `FinancialAccount`, `ImportedTransaction`, `ConnectionStatus` enum, `AccountType` enum, `CreditCardBill`, `BillStatus` enum)
- **dto/** — request/response objects with Bean Validation annotations
- **mapper/** — entity ↔ DTO conversion (manual, no MapStruct)
- **security/** — `JwtService` (JJWT), `JwtFilter` (extracts userId from token), `JwtAuthenticationEntryPoint`
- **exception/** — `GlobalExceptionHandler` returns `ApiError` with optional `errors` list (`FieldErrorDTO`) for validation failures
- **openfinance/** — provider abstraction (`OpenFinanceProvider` interface + `AccountData`, `TransactionData`, `BillData`, `ItemStatus` records; `PluggyCategory` enum for category policy) and `pluggy/` implementation (`PluggyProvider`, `PluggyProperties`)
- **scheduler/** — `OpenFinanceSyncScheduler` (cron `0 0 3 * * *`, syncs all ACTIVE connections)

**Auth flow:** `JwtService` embeds `userId` as the JWT subject. `JwtFilter` extracts it and sets it as the Spring Security principal (`Long userId`). Services retrieve the current user via `AuthService.getAuthenticatedUser()`, which reads from `SecurityContextHolder` and fetches the `User` entity. Public endpoints: `/auth/login`, `/auth/register`, `/auth/refresh`.

**Schema ownership:** Hibernate is set to `ddl-auto=validate` — the schema is managed exclusively by Flyway migrations in `resources/db/migration/`. Never let Hibernate create or alter tables.

**Database tables:**
- `app_user` — the user table; named `app_user` because `user` is a reserved word in PostgreSQL
- `category` — shared (is_default=true, user_id=null) and user-specific (is_default=false, user_id set); unique index on `(user_id, name)` for non-default categories; 5 default categories seeded in V2 migration (Alimentação, Lazer, Saúde, Educação, Despesas Domésticas)
- `transaction` — amount `NUMERIC(15,2)`, description optional max 60 chars, category optional (SET NULL on category delete); `imported BOOLEAN NOT NULL DEFAULT FALSE`; `source_account_id` FK to `financial_account` (SET NULL on delete); `bill_id` FK to `credit_card_bill` (SET NULL on delete) — set only for card purchases attached to a closed provider bill; `installment_number`/`total_installments` (provider installment metadata); `provider_category VARCHAR(80)` — raw provider category kept verbatim (e.g. Pluggy's "Investments"), NULL for manual transactions (see "Investments hiding")
- `user_profile` — optional secondary profile; created on first update
- `refresh_token` — one per user; generating a new one revokes the previous
- `friendship` — directed friend relationship; `requester_id`/`addressee_id` (both FK `app_user`, CASCADE), `status` (`PENDING`/`ACCEPTED`), `created_at`/`updated_at`. DB enforces `requester_id <> addressee_id` and unique `(requester_id, addressee_id)`
- `financial_connection` — one row per institution connected by a user; `provider` (e.g. `"PLUGGY"`), `external_item_id` (Pluggy item ID), `status` (`ACTIVE`/`ERROR`/`DISCONNECTED`). Disconnect is a soft-delete (status → DISCONNECTED) to preserve import history.
- `financial_account` — accounts imported from a connection; `external_account_id`, `type` (`CONTA_CORRENTE`/`POUPANCA`/`CARTAO_CREDITO`), `current_balance`, `currency`. Updated on every sync.
- `imported_transaction` — deduplication ledger; unique on `(provider, external_transaction_id)`. `transaction_id` is SET NULL when the transaction is deleted by the user, so the row persists and prevents re-import on future syncs.
- `credit_card_bill` — one row per provider statement **plus** one open "transition" row per card. `account_id` FK (CASCADE), `provider`, `external_bill_id` (the Pluggy bill id; **NULL** identifies the transition bill), `due_date` (NULL only for a transition on a card with no closed bill yet), `total_amount NUMERIC(15,2)`, `status` (`OPEN` = transition / `CLOSED` = provider bill), `bill_sequence` (NULL for the transition; sequential per user across closed bills), `custom_name` (rename). Unique `(provider, external_bill_id)` for closed bills; partial unique index `uq_ccb_open_transition (account_id) WHERE external_bill_id IS NULL` enforces **at most one transition bill per card**. Rows are never deleted, so when a bill falls out of Pluggy's 12-bill window we become its frozen source of truth. See "Credit-card bills & the transition bill".

**Category constraint:** enforced at DB level — `is_default=true` requires `user_id IS NULL`; `is_default=false` requires `user_id IS NOT NULL`. Lives in `V2__create_category_table.sql`. `GET /categories` returns both default and the authenticated user's own categories. `POST /categories` creates a user-owned category; `POST /categories/default` creates a shared category.

**JWT expiry:** 15 minutes (hardcoded in `JwtService`). Refresh tokens expire in 7 days (one per user, rotating). Every use rotates the token. `POST /auth/refresh` is public; `POST /auth/logout` requires auth and revokes the stored token.

**User profile:** `UserProfile` is a separate entity/table (`user_profile`) with a `@OneToOne` lazy relation to `User`. `ProfileService.getProfile()` returns an empty profile if no record exists (`orElseGet(UserProfile::new)`); `updateProfile` and `uploadPhoto` both upsert. Photo is stored as `BYTEA` alongside its MIME type (`photo_type`) and returned as base64 in `ProfileResponseDTO`. The `uploadPhoto` endpoint accepts `multipart/form-data` (not JSON), so it cannot go through `apiFetch` — uses raw `fetch` with a manual `Authorization` header.

**Friends (social):** `Friendship` models a directed relationship with a `status` enum (`PENDING`, `ACCEPTED`) — the requester sends, the addressee responds:
- `POST /friends/requests` `{ email }` — looks the user up by email and creates a `PENDING` request from the authenticated user (requester) to that user (addressee). Rejects self-add / duplicate / already-friends with `ConflictException`, unknown email with `NotFoundException`. Reverse-direction duplicate (B invites A after A already invited B) is caught in `FriendshipService` via `findBetween`, since the DB unique constraint only covers a single direction.
- `GET /friends/requests` — pending requests where the authenticated user is the addressee (returns the requester's name/email plus the request id = friendship id, used to accept/reject).
- `POST /friends/requests/{id}/accept` — addressee-only; flips `PENDING` → `ACCEPTED` (sets `updated_at`) and returns the new `FriendDTO`.
- `POST /friends/requests/{id}/reject` — addressee-only; **deletes** the row (no `REJECTED` state is kept, so a fresh invite can be sent again later).
- `GET /friends` — only `ACCEPTED` friends. Accept/reject share `getPendingRequestForAddressee`, which enforces addressee ownership (`ForbiddenException`) and `PENDING` status (`ConflictException`).

`FriendDTO` / `FriendRequestItemDTO` carry a **display name + avatar pulled from the person's `UserProfile`**, not the raw `app_user` row: `name` is `nickname` (falls back to the registration `name` only when no nickname is set — so a user's real name is never exposed once they pick a nickname), plus `photoBase64`/`photoType` (same base64 encoding as `ProfileResponseDTO`). `FriendshipService` batch-loads the relevant profiles in one query (`UserProfileRepository.findByUserIdIn`) and builds a `userId → UserProfile` map to avoid N+1; the `FriendshipMapper` takes the resolved `User` + its (nullable) `UserProfile`.

**Exception handling:** `GlobalExceptionHandler` maps `NotFoundException` → 404, `ForbiddenException` → 403, `ConflictException` → 409, `MethodArgumentNotValidException` → 400 with field-level errors, generic `Exception` → 500. All responses use the `ApiError` shape.

**Open Finance (Pluggy integration):**
- `OpenFinanceProvider` interface in `openfinance/` abstracts the external provider (RA-001). `PluggyProvider` calls Pluggy's REST API via Spring `RestClient`; no Pluggy SDK dependency (SDK is GitHub Packages only). API key is cached in memory with 2-hour TTL and refreshed automatically.
- Pluggy endpoints used: `POST /auth` (API key), `POST /connect_token`, `GET /items/{id}`, `GET /accounts?itemId=`, `GET /v2/transactions?accountId=&dateFrom=&dateTo=` (cursor pagination via `next` field), `GET /bills?accountId=`. **Param names matter:** transactions use `dateFrom`/`dateTo` (not `from`/`to`). A transaction carries `creditCardMetadata.billId` (links it to a statement), `installmentNumber`/`totalInstallments`, and a `category` string.
- **Connect flow:** frontend calls `POST /open-finance/connect-token` → opens Pluggy widget → on success sends `itemId` to `POST /open-finance/connections` → backend runs initial sync (last 90 days).
- **Sync logic:** `OpenFinanceSyncService.syncConnection(connection, from)` upserts accounts (balance updated every run). Per account it branches on type:
  - **Non-card** (`importNewTransactions`): import transactions absent from `imported_transaction` (matched by `provider + externalTransactionId`); on already-imported rows it only **refreshes provider metadata** (`provider_category`) — never user-editable fields.
  - **Card** (`syncBills` → `importCardTransactions` → `syncTransitionBill`, in that order): see "Credit-card bills".
  - User edits are never overwritten — amount/type/date/category of an existing `transaction` are never touched after creation; only provider metadata (bill link, category) is re-derived. Deleted transactions leave their `imported_transaction` row intact (SET NULL), preventing re-import.
- **Daily sync:** `OpenFinanceSyncScheduler` cron at 03:00, fetches last 2 days per ACTIVE connection; errors mark the connection as ERROR but don't stop other connections from syncing. (Note: 2-day windows mean `provider_category`/bill-link backfill of older rows needs a manual per-connection sync, which uses the 90-day window.)
- `PluggyProperties` reads `pluggy.client-id`, `pluggy.client-secret`, `pluggy.base-url` from `application.properties`. Registered via `@EnableConfigurationProperties` in `OpenFinanceConfig`.
- **Pluggy trial accounts:** must pass `includeSandbox={true}` to the `PluggyConnect` widget to see sandbox connectors.

**Credit-card bills & the transition bill (`syncBills`, `syncTransitionBill`, `BillService`):**

The hard-won product insight: **Pluggy only returns a bill once the statement has CLOSED.** The currently-open cycle has *no* provider bill — its purchases arrive via `/v2/transactions` with `billId == null` not because they're "unprocessed", but because the open bill doesn't exist in the API yet. So:
- **Every bill from `/bills` is CLOSED**, even the one with the nearest due date (a past due date can't be an open bill). `syncBills` upserts each by `external_bill_id` and forces `status = CLOSED`. It never marks anything OPEN.
- **The open cycle is modeled as a synthetic "transition bill"** — one persisted `credit_card_bill` row per card with `external_bill_id = NULL`, `status = OPEN`. `syncTransitionBill` find-or-creates it and, on every sync, recomputes:
  - `total_amount` = **Σ of the card's EXPENSE transactions with `bill_id IS NULL`** (the orphans). INCOME orphans are excluded on purpose — a card INCOME with null billId is the *payment* of a previous closed bill (observed: an INCOME exactly equal to the prior bill's total), so it must not reduce the open cycle. (Refund/estorno handling is deferred — see `requisito-estornos.txt`.)
  - `due_date` = most recent **closed** bill's due date `+ 1 month` (`LocalDate.plusMonths(1)`, which clamps day overflow), or `NULL` if the card has no closed bill yet.
- **Why persisted (Option B), not virtual:** chosen so the open bill has a stable id (renamable per card — important with multiple cards/institutions) and so the total isn't recomputed on every GET. The rename (`custom_name`) lives on the perpetual transition row, so it stays attached to "this card's open bill" across cycles instead of migrating to a historical bill.
- **Cycle close is emergent, no promotion logic:** when a cycle closes, Pluggy returns the new closed bill (upserted as its own CLOSED row) and its transactions now carry a `billId` (so `importCardTransactions` links them via `bill_id`); they drop out of the transition's orphan sum automatically, and the transition recomputes to the next cycle's orphans.
- `importCardTransactions`: `billId != null` → link to the matching closed bill (`bill_id` set); `billId == null` → **stays `bill_id NULL`** (an orphan; no "transport" to the open bill — that old approach caused 3×-inflated totals by piling misattributed past purchases onto one bill).
- `recomputeBillSequence` numbers **closed bills only** (`external_bill_id IS NOT NULL`), per user, ascending by due date; the transition has no sequence.
- **Bill totals are the provider's, not `sum(items)`** — a real statement has lines (interest, IOF, annuity, FX, carried balance) that never arrive as discrete transactions, so imported items are a possibly-incomplete subset. `BillService.toResponse` therefore always shows `total_amount` and fetches items two ways: closed bill → `findByBillId`; transition → the same orphan-EXPENSE query that feeds its total. Full rationale is in the `BillService.isTransition` Javadoc.
- **Endpoints:** `GET /transactions` returns bills inline as ledger entries (below). `PATCH /bills/{id}` renames (owner-only via the bill's account → user; blank name clears `custom_name`). Bills are read-only otherwise.
- **Business rules:** a card transaction can only have its **description** edited (it's imported); a transaction attached to a bill (`bill != null`) **cannot be deleted** (`ConflictException` "Transações de fatura não podem ser removidas").

**Transactions ledger (`GET /transactions`, `TransactionService.getByAuthenticatedUser`):**
- Returns `List<LedgerEntryDTO>` — a tagged union (`type` = `TRANSACTION` | `BILL`) merging standalone transactions and one aggregated entry per credit-card bill (closed + transition) due in the period.
- **Period:** `?year=&month=` (both optional; default = current month). Filters by the persisted `date` over the full calendar month (`YearMonth.atDay(1)` .. `atEndOfMonth()`), inclusive.
- **Standalone query** (`findVisibleLedgerEntries`) excludes: (a) transactions tied to a bill (`bill IS NULL`), (b) **all credit-card-account transactions** (`sourceAccount.type <> CARTAO_CREDITO`) — card movements only ever appear inside a bill, never loose, which is also what hides card payment INCOME, and (c) hidden provider categories (investments). Manual transactions (null source account, null category) always pass.
- Entries are sorted by date descending (stable: bills sort by `due_date`, transactions by `date`, ties broken by id).
- **Edit (`PUT /transactions/{id}`):** for `imported` transactions only the description may change — `ensureOnlyDescriptionChanged` throws `ConflictException` if amount/type/date/category differ. Manual transactions edit all fields.

**Investments hiding (`PluggyCategory`):**
- The provider category is captured verbatim into `transaction.provider_category` on import (it was previously discarded). The `PluggyCategory` enum is the single policy seam mapping the provider taxonomy to app behavior: today it flags which categories are hidden from the main ledger (`INVESTMENTS`, matched case-insensitively against a few label variants).
- `findVisibleLedgerEntries` excludes rows whose `LOWER(provider_category)` is in `PluggyCategory.hiddenLedgerLabels()`. Kept as an enum (not a DB table) — it's a code-level policy, and storing the raw category future-proofs a dedicated investments view without a re-sync. The label set has been confirmed against live Pluggy data.

**Mock CRUD (dev-only, `MockController` + `/mock` frontend):**
- `/{concept}` CRUD (`connections`, `accounts`, `bills`, `transactions`) to seed imported-style data without hitting Pluggy. User-scoped; auto-generates external ids. Relation fields are selects populated from existing DB rows. Bills can only be created for `CARTAO_CREDITO` accounts (`ConflictException` otherwise). **Should be gated/removed before any real deploy.**

## Frontend architecture

Next.js 16 App Router with two route groups:

- `(auth)/` — login and register pages, wrapped by `AuthLayout`
- `(dashboard)/` — protected pages; `layout.tsx` checks for `token` in `localStorage` and redirects to `/login` if absent

**Dashboard routes:**
- `/transactions` — main page; ledger of `LedgerEntryDTO` (transactions + aggregated bill rows) with summary cards, a month/year period filter, create/delete modals, and inline description editing. Summary `totalExpense` adds each `bill.total`; card transactions never appear as standalone rows (they're inside bills), so there's no double-count.
- `/open-finance` — connected institutions and imported accounts; "Conectar banco" button opens the Pluggy Connect widget; per-connection sync and disconnect actions
- `/profile` — secondary profile fields (nickname, birth date, monthly income, marital status, photo upload)
- `/mock` — **dev-only** UI over `MockController` (`/mock/{concept}`) to seed imported data; see "Mock CRUD"
- `/preferences` — placeholder, not yet implemented

**Data fetching pattern:** custom hooks (`useTransactions`, `useCategories`, `useProfile`, `useFriends`, `useOpenFinance`) call service functions which use `apiFetch` from `services/api.ts`. `apiFetch` is the single place that attaches the `Authorization: Bearer <token>` header. Any new API call should go through it, not raw `fetch` — except multipart uploads (`uploadPhoto` in `services/profile.ts`), which must set the `Authorization` header manually and omit `Content-Type` so the browser sets the multipart boundary.

**Token storage:** access token in `localStorage["token"]`, refresh token in `localStorage["refreshToken"]`. `useAuth.login` persists both and redirects to `/transactions`. `apiFetch` intercepts 401 responses, attempts a silent refresh via `tryRefreshToken` from `lib/auth.ts` (concurrent requests share a single in-flight promise to avoid races), retries the original request once, and calls `logout()` if the refresh also fails.

**Two logout paths — keep them distinct:**
- `lib/auth.ts:logout` — module-level function; calls `POST /auth/logout`, clears localStorage, then does `window.location.href = "/login"`. Used in `Header` and by `apiFetch` on failed refresh — can be called outside React.
- `useAuth().logout` — React hook; clears localStorage and uses `router.push("/login")`. Do not use for the API call flow.

**Profile state:** `ProfileContext` (in `src/context/ProfileContext.tsx`) wraps the dashboard layout and holds a single shared profile instance fetched once on mount. `useProfile` is a thin wrapper around this context — never call `getProfile()` directly in components; always use `useProfile()` so the Header and profile page stay in sync. After mutating the profile (updateProfile, uploadPhoto), call `setProfile(updated)` from the context to propagate changes.

**UI components (`src/components/ui/`):**
- `Select` — custom dropdown; props: `value`, `onChange`, `options` (`{ value, label }[]`), `placeholder`, `disabled`, `actions` (`{ label, onSelect, className }[]`). The `actions` prop adds footer entries separated by a border (used for inline category creation).
- `Modal` — simple overlay wrapper.
- `MoneyInput` — currency input for BRL amounts. Stores value in cents internally; digits are appended right-to-left (ATM style). Backspace removes the last digit. Displays as `R$ X.XXX,XX`. Takes `value: number` (as reais, not cents) and `onChange: (value: number) => void`.

**Feature components:**
- `src/app/(dashboard)/categories/CategorySelect.tsx` — wraps `Select` with inline category creation. Shows a "+ Nova Categoria" action in the dropdown footer; on select, renders an inline input to create the category via `POST /categories`. Maintains a `localExtra` state to show newly created categories before the parent reloads `useCategories`.
- `src/app/(dashboard)/open-finance/page.tsx` — Open Finance page. Fetches connections + accounts via `useOpenFinance`. "Conectar banco" calls `POST /open-finance/connect-token`, renders `<PluggyConnect>` (loaded with `dynamic({ ssr: false })` from `react-pluggy-connect`), and on widget success posts `itemId` to `POST /open-finance/connections`. Uses `includeSandbox={true}` for trial Pluggy accounts.
- `src/app/(dashboard)/transactions/components/BillRow.tsx` — renders a bill ledger entry: collapsible header (card icon, name, item count, "• Aberta" when `status === "OPEN"`, due day, total in `--color-expense`, rename pencil → `renameBill`). Expands to `BillItemRow`s; each item shows an installment badge (`Parcela X/Y` when `totalInstallments > 1`) and supports inline description editing.
- `src/app/(dashboard)/transactions/components/{useInlineDescription.ts,InlineDescriptionBar.tsx}` — shared "edit only the description" state machine + UI, used by table rows and bill items. `useInlineDescription(tx, onSaved, onError)` calls `updateTransactionDescription` (a `PUT /transactions/{id}` that resends every locked field unchanged and only swaps the description). Enter saves, Escape cancels.
- `src/app/(dashboard)/transactions/components/PeriodSelector.tsx` — subtle calendar-icon button next to "Nova transação" opening a dropdown with year + month "carousels" (press-hold-drag with mouse + arrows; years clamp at 1970, months wrap Dec↔Jan adjusting the year). Drives the `?year=&month=` of `useTransactions`.
- `src/lib/format.ts` — `formatCurrency` (BRL) and `formatDay` (renders a date as `Dia {dd}`). `src/services/bills.ts` — `renameBill`. `src/services/mock.ts` — mock CRUD client.
- `src/components/layout/FriendsMenu.tsx` — MSN-style two-avatar button in the `Header` that opens a dropdown friends list. A count badge on the button shows pending incoming requests; the dropdown lists a "Convites recebidos" section (accept ✓ / reject ✕ per item) above the friends list. List/request items render the friend's profile photo via `getPhotoSrc` (falling back to initials) and show the nickname — both come resolved from the backend DTO. Empty state shows an avatar-with-question-mark icon; an "Adicionar amigo" footer reveals an inline email form that calls `POST /friends/requests` and shows a transient success banner (or inline error read from `ApiError.fields` by field name, falling back to `message`). `useFriends` loads friends + requests in parallel and exposes `accept`/`reject` that mutate local state (accept moves the item from requests into friends). Has its own click-outside/Escape handling, independent of the user menu's.

**Type utilities:**
- `src/types/profile.ts:getPhotoSrc(profile)` — constructs a `data:${photoType};base64,${photoBase64}` URL from profile fields. Returns `null` if either field is absent. Used by `Header` and `ProfilePage` to render the avatar.

**Styling:** Tailwind CSS v4 with `@tailwindcss/postcss`. CSS custom properties defined in `app/globals.css` (`:root`) drive the color system — graphite dark theme with turquoise accent (`--color-teal: #1ec9b4`). Use `var(--color-*)` in arbitrary Tailwind values (e.g. `bg-[var(--color-surface)]`). Key tokens: `--color-bg`, `--color-surface`, `--color-raised`, `--color-border`, `--color-muted`, `--color-secondary`, `--color-text`, `--color-teal`, `--color-teal-dark`, `--color-income`, `--color-expense`.

**Date handling:** always use local-timezone methods (`getFullYear()`, `getMonth()`, `getDate()`) when formatting dates for `<input type="date">` — never `toISOString()`, which returns UTC and shifts the date for users in negative UTC offsets.
