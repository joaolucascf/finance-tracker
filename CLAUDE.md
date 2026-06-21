# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Personal finance tracker with a Spring Boot backend and Next.js frontend backed by PostgreSQL.

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
- **controller/** — REST endpoints (`AuthController`, `TransactionController`, `CategoryController`, `ProfileController`, `FriendshipController`, `OpenFinanceController`)
- **service/** — business logic; receives DTOs, operates on entities
- **repository/** — JPA repositories (Spring Data)
- **entity/** — JPA entities (`User`, `UserProfile`, `RefreshToken`, `Transaction`, `Category`, `TransactionType` enum, `Friendship`, `FriendshipStatus` enum, `FinancialConnection`, `FinancialAccount`, `ImportedTransaction`, `ConnectionStatus` enum, `AccountType` enum)
- **dto/** — request/response objects with Bean Validation annotations
- **mapper/** — entity ↔ DTO conversion (manual, no MapStruct)
- **security/** — `JwtService` (JJWT), `JwtFilter` (extracts userId from token), `JwtAuthenticationEntryPoint`
- **exception/** — `GlobalExceptionHandler` returns `ApiError` with optional `errors` list (`FieldErrorDTO`) for validation failures
- **openfinance/** — provider abstraction (`OpenFinanceProvider` interface + `AccountData`, `TransactionData`, `ItemStatus` records) and `pluggy/` implementation (`PluggyProvider`, `PluggyProperties`)
- **scheduler/** — `OpenFinanceSyncScheduler` (cron `0 0 3 * * *`, syncs all ACTIVE connections)

**Auth flow:** `JwtService` embeds `userId` as the JWT subject. `JwtFilter` extracts it and sets it as the Spring Security principal (`Long userId`). Services retrieve the current user via `AuthService.getAuthenticatedUser()`, which reads from `SecurityContextHolder` and fetches the `User` entity. Public endpoints: `/auth/login`, `/auth/register`, `/auth/refresh`.

**Schema ownership:** Hibernate is set to `ddl-auto=validate` — the schema is managed exclusively by Flyway migrations in `resources/db/migration/`. Never let Hibernate create or alter tables.

**Database tables:**
- `app_user` — the user table; named `app_user` because `user` is a reserved word in PostgreSQL
- `category` — shared (is_default=true, user_id=null) and user-specific (is_default=false, user_id set); unique index on `(user_id, name)` for non-default categories; 5 default categories seeded in V2 migration (Alimentação, Lazer, Saúde, Educação, Despesas Domésticas)
- `transaction` — amount `NUMERIC(15,2)`, description optional max 60 chars, category optional (SET NULL on category delete); `imported BOOLEAN NOT NULL DEFAULT FALSE`; `source_account_id` FK to `financial_account` (SET NULL on delete)
- `user_profile` — optional secondary profile; created on first update
- `refresh_token` — one per user; generating a new one revokes the previous
- `friendship` — directed friend relationship; `requester_id`/`addressee_id` (both FK `app_user`, CASCADE), `status` (`PENDING`/`ACCEPTED`), `created_at`/`updated_at`. DB enforces `requester_id <> addressee_id` and unique `(requester_id, addressee_id)`
- `financial_connection` — one row per institution connected by a user; `provider` (e.g. `"PLUGGY"`), `external_item_id` (Pluggy item ID), `status` (`ACTIVE`/`ERROR`/`DISCONNECTED`). Disconnect is a soft-delete (status → DISCONNECTED) to preserve import history.
- `financial_account` — accounts imported from a connection; `external_account_id`, `type` (`CONTA_CORRENTE`/`POUPANCA`/`CARTAO_CREDITO`), `current_balance`, `currency`. Updated on every sync.
- `imported_transaction` — deduplication ledger; unique on `(provider, external_transaction_id)`. `transaction_id` is SET NULL when the transaction is deleted by the user, so the row persists and prevents re-import on future syncs.

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
- Pluggy endpoints used: `POST /auth` (API key), `POST /connect_token`, `GET /items/{id}`, `GET /accounts?itemId=`, `GET /v2/transactions?accountId=&dateFrom=&dateTo=` (cursor pagination via `next` field).
- **Connect flow:** frontend calls `POST /open-finance/connect-token` → opens Pluggy widget → on success sends `itemId` to `POST /open-finance/connections` → backend runs initial sync (last 90 days).
- **Sync logic:** `OpenFinanceSyncService.syncConnection()` upserts accounts (balance updated every run), then imports only transactions absent from `imported_transaction` (checked by `provider + externalTransactionId`). User edits are never overwritten — existing `transaction` rows are never touched after creation. Deleted transactions leave their `imported_transaction` row intact (SET NULL), preventing re-import.
- **Daily sync:** `OpenFinanceSyncScheduler` cron at 03:00, fetches last 2 days per ACTIVE connection; errors mark the connection as ERROR but don't stop other connections from syncing.
- `PluggyProperties` reads `pluggy.client-id`, `pluggy.client-secret`, `pluggy.base-url` from `application.properties`. Registered via `@EnableConfigurationProperties` in `OpenFinanceConfig`.
- **Pluggy trial accounts:** must pass `includeSandbox={true}` to the `PluggyConnect` widget to see sandbox connectors.

## Frontend architecture

Next.js 16 App Router with two route groups:

- `(auth)/` — login and register pages, wrapped by `AuthLayout`
- `(dashboard)/` — protected pages; `layout.tsx` checks for `token` in `localStorage` and redirects to `/login` if absent

**Dashboard routes:**
- `/transactions` — main page; transaction list with summary cards and create/delete modals
- `/open-finance` — connected institutions and imported accounts; "Conectar banco" button opens the Pluggy Connect widget; per-connection sync and disconnect actions
- `/profile` — secondary profile fields (nickname, birth date, monthly income, marital status, photo upload)
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
- `src/components/layout/FriendsMenu.tsx` — MSN-style two-avatar button in the `Header` that opens a dropdown friends list. A count badge on the button shows pending incoming requests; the dropdown lists a "Convites recebidos" section (accept ✓ / reject ✕ per item) above the friends list. List/request items render the friend's profile photo via `getPhotoSrc` (falling back to initials) and show the nickname — both come resolved from the backend DTO. Empty state shows an avatar-with-question-mark icon; an "Adicionar amigo" footer reveals an inline email form that calls `POST /friends/requests` and shows a transient success banner (or inline error read from `ApiError.fields` by field name, falling back to `message`). `useFriends` loads friends + requests in parallel and exposes `accept`/`reject` that mutate local state (accept moves the item from requests into friends). Has its own click-outside/Escape handling, independent of the user menu's.

**Type utilities:**
- `src/types/profile.ts:getPhotoSrc(profile)` — constructs a `data:${photoType};base64,${photoBase64}` URL from profile fields. Returns `null` if either field is absent. Used by `Header` and `ProfilePage` to render the avatar.

**Styling:** Tailwind CSS v4 with `@tailwindcss/postcss`. CSS custom properties defined in `app/globals.css` (`:root`) drive the color system — graphite dark theme with turquoise accent (`--color-teal: #1ec9b4`). Use `var(--color-*)` in arbitrary Tailwind values (e.g. `bg-[var(--color-surface)]`). Key tokens: `--color-bg`, `--color-surface`, `--color-raised`, `--color-border`, `--color-muted`, `--color-secondary`, `--color-text`, `--color-teal`, `--color-teal-dark`, `--color-income`, `--color-expense`.

**Date handling:** always use local-timezone methods (`getFullYear()`, `getMonth()`, `getDate()`) when formatting dates for `<input type="date">` — never `toISOString()`, which returns UTC and shifts the date for users in negative UTC offsets.
