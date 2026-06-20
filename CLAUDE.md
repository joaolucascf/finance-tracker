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
```

## Backend commands (run from `/backend`)

```bash
mvn test                              # all tests
mvn test -Dtest=TransactionServiceTest   # single test class
mvn test -Dtest=TransactionIntegrationTest#methodName  # single method
mvn spring-boot:run                   # run with default profile
```

Integration tests require Docker (they spin up a real PostgreSQL via Testcontainers).

## Frontend commands (run from `/frontend`)

```bash
npm run dev        # dev server
npm run build      # production build
npm run lint       # ESLint
npx tsc --noEmit   # type check
```

## Backend architecture

Layered Spring Boot app under `com.joaolucas.finance_tracker`:

- **controller/** — REST endpoints (`AuthController`, `TransactionController`, `CategoryController`)
- **service/** — business logic; receives DTOs, operates on entities
- **repository/** — JPA repositories (Spring Data)
- **entity/** — JPA entities (`User`, `Transaction`, `Category`, `TransactionType` enum)
- **dto/** — request/response objects with Bean Validation annotations
- **mapper/** — entity ↔ DTO conversion (manual, no MapStruct)
- **security/** — `JwtService` (JJWT), `JwtFilter` (extracts userId from token), `SecurityConfiguration`
- **exception/** — `GlobalExceptionHandler` returns `ApiError` with optional `fields` list for validation errors

**Auth flow:** `JwtService` embeds `userId` as the JWT subject. `JwtFilter` extracts it and sets it as the Spring Security principal. Services retrieve the current user via `SecurityContextHolder`.

**Schema ownership:** Hibernate is set to `ddl-auto=validate` — the schema is managed exclusively by Flyway migrations in `resources/db/migration/`. Never let Hibernate create or alter tables.

**Category constraint:** enforced at DB level — `is_default=true` requires `user_id IS NULL`; `is_default=false` requires `user_id IS NOT NULL`. This check lives in `V2__create_category_table.sql`.

**JWT expiry:** 15 minutes (hardcoded in `JwtService`). The frontend does not handle token refresh.

## Frontend architecture

Next.js 16 App Router with two route groups:

- `(auth)/` — login and register pages, wrapped by `AuthLayout`
- `(dashboard)/` — protected pages; `layout.tsx` checks for `token` in `localStorage` and redirects to `/login` if absent

**Data fetching pattern:** custom hooks (`useTransactions`, `useCategories`) call service functions which use `apiFetch` from `services/api.ts`. `apiFetch` is the single place that attaches the `Authorization: Bearer <token>` header. Any new API call should go through it, not raw `fetch`.

**Token storage:** JWT is stored in `localStorage` under the key `"token"`. The `useAuth` hook and `lib/auth.ts` handle login/logout.

**Styling:** Tailwind CSS v4 with `@tailwindcss/postcss`. CSS custom properties defined in `app/globals.css` (`:root`) drive the color system — graphite dark theme with turquoise accent. Use `var(--color-*)` in arbitrary Tailwind values (e.g. `bg-[var(--color-surface)]`).
