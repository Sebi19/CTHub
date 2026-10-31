Spring Boot 4 (Java 21) backend at the repo root + React 18/TypeScript/Vite/Mantine 7 frontend in `frontend/`. The Maven build bundles the frontend into the backend JAR.

## Core Commands

| Task | Command (from repo root unless noted) |
|---|---|
| Start DB (Postgres 16) | `docker-compose up -d` |
| Install frontend deps | `cd frontend; npm install` |
| Run backend | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` (Unix: `./mvnw ... -Dspring-boot.run.profiles=local`) |
| Run frontend dev server | `cd frontend; npm run dev` (port 5173, proxies `/api` → 8080) |
| Backend tests | `.\mvnw.cmd test` |
| Frontend lint | `cd frontend; npm run lint` |
| Frontend typecheck/build | `cd frontend; npm run build` (also runs as the Husky pre-commit hook) |
| Regenerate API client | `cd frontend; npm run gen:api` (backend must be running) |
| Full package | `.\mvnw.cmd clean package` (`-Dskip.frontend.build=true` to skip the frontend build) |

No formatter is configured; match the surrounding style.

## Hard Directives

- **npm only** (lockfiles are `package-lock.json`). The root `package.json` only exists for Husky.
- **Never hand-edit `frontend/src/api/generated.ts`.** After changing a backend DTO or controller, run `npm run gen:api`.
- Call the backend only through the shared `client` from `frontend/src/api.ts` (`client.api.<operation>()`). Don't use raw axios or fetch.
- Frontend components use **named exports only** and live under `frontend/src/features/<feature>/`.
- Every user-facing string goes through `useTranslation()`. Add keys to **both** `i18n/locales/de.ts` (the source type) and `en.ts`. German is the fallback language.
- Backend layering: `controller → service → repository`, with entity↔DTO conversion done only by **MapStruct** `@Mapper(componentModel = "spring")` interfaces in `mapper/`. Use Lombok (`@RequiredArgsConstructor`) for injection.
- Controllers are mapped under `/api/...`, default to `@PreAuthorize("isAuthenticated()")`, and open public endpoints explicitly with `permitAll()`.
- Hibernate runs with `ddl-auto: validate`. **Every schema change needs a new Flyway file** `src/main/resources/db/migration/V<n+1>__<desc>.sql`. Never edit an existing `V*` file.
- Put one-off data fixes in `org.cthub.backend.migration` as `CommandLineRunner` components, not in SQL migrations.
- Never commit real secrets. Production config comes from environment variables (`PGHOST`, `ADMIN_PASSWORD`, `ZEPTOMAIL_TOKEN`, …).
- Mail templates (`src/main/resources/mail/templates/**/*.html`) use a dedicated Thymeleaf engine (`MailAutoConfig`) restricted to resolvable patterns `*.html`/`*.txt`. Fragment references (`th:replace`/`th:insert`) **must include the file extension** (e.g. `~{layout.html :: page(...)}`), otherwise they fail to resolve at render time — this only surfaces when a template is actually rendered, not at context-load.
- The `.txt` mail templates run in Thymeleaf's **TEXT mode, which forbids fragment selectors** — `~{footer.txt :: footer}` throws at render time. Text templates may only insert whole files (`~{footer.txt}`), so `footer.txt` has no `th:fragment` wrapper.
- `ThymeleafMailRenderer` injects `baseUrl` and `currentYear` into every template. `currentYear` is deliberately a **String**: as an int, MessageFormat would render it with locale grouping ("2.026"). The mail logo must stay a PNG under `frontend/public/` (SVG is unsupported in Outlook/Gmail, and `src/assets/` URLs are content-hashed).
- `ProfileCompletionInterceptor` is a **hard** gate — reads included. Only `/api/auth/**` is exempt (`ALLOWED_PREFIX`), so any new "get yourself unstuck" endpoint must live there; gating `/api/auth/me` deadlocks the frontend, and `OPTIONS` must stay exempt or CORS preflight masks the 403.
- Native `@Query`s that already express locking in SQL (e.g. `for update skip locked`) must **not** also carry `@Lock`/`@QueryHints` — Hibernate rejects `setLockMode` on native queries at runtime (see `MailOutboxRepository.claimDueBatch`).
- `User.firstName`/`lastName`/`password` are **nullable** (accounts exist from the moment the e-mail is verified, before the profile is completed). `User.getPassword()` returns a BCrypt placeholder when no password is set, because Spring Security's `DaoAuthenticationProvider` fails opaquely on `null`; use `hasPassword()`/`isProfileComplete()` for real checks, and never persist `getPassword()` (Hibernate uses field access, so the placeholder never reaches the DB).
- `user_token.type` has a Postgres CHECK constraint. **Adding a `UserTokenType` value requires a Flyway migration** that drops and recreates that constraint.
- `mail_outbox.dedupe_key` is a *permanent* unique column — it is not a time-window. Never derive it from something that legitimately repeats (e.g. a user id), or you silently suppress every resend forever. Leave it `null` and bound abuse with the rate limiter instead.
- springdoc must stay on the **3.x** line (Spring Boot 4 / Spring Framework 7). 2.x throws `NoSuchMethodError: ControllerAdviceBean.<init>(Object)` on `/v3/api-docs` as soon as a `@RestControllerAdvice` bean exists.
- springdoc names nested enum schemas by their **simple name**, so two different inner enums called `Status` collapse into one broken TS enum in `api/generated.ts`. Give nested enums globally unique names.
- Spring Boot 4 ships **Jackson 3** (`tools.jackson.databind`); there is no `com.fasterxml.jackson.databind.ObjectMapper` bean to inject.
- `mvnw spring-boot:run` runs `npm run build` first. Pass `-Dskip.frontend.build=true` when the frontend is mid-edit.

## Routing (read before modifying)

- **Database / persistence:** Before touching `model/` entities or repositories, you MUST read `application.yaml` and the latest files in `db/migration/`.
- **Data import (scraper / Flow API sync):** Before changing `service/scraper/` or `service/syncer/`, you MUST read the matching `*Scheduler`, `*Service` and `*Persister` classes. These jobs write production data on a schedule.
- **Auth / security:** Before changing endpoints, login, or CORS, you MUST read `config/SecurityConfig.java`, the `application-<profile>.yaml` files, and `frontend/src/features/auth/AuthContext.tsx`.
- **Login / sessions / step-up:** Before changing any of these, you MUST read `auth/attempt/LoginAttemptService.java`, `auth/session/AuthSessionService.java`, `auth/session/SessionAuthenticationFilter.java`, `controller/AuthController.java`, `controller/LoginAttemptController.java`, `config/ProfileCompletionInterceptor.java`, and the README's *Authentication & Registration* section.
- `POST /api/auth/identify` and `POST /api/auth/attempts` must answer identically for known and unknown e-mails (no account enumeration). Unknown addresses get an account only once the challenge completes.
- Only browser A (holding the `ct_attempt` nonce cookie) can redeem an `APPROVED` attempt. Polling endpoints and `GET /api/auth/link` must **never** establish a session or mutate state — mail scanners prefetch `GET`s.
- A wrong number picked on any channel rejects the **whole** attempt; never allow a second pick.
- TOTP is a second factor after a password, and a step-up method — never a standalone login factor.
- Refresh rotation: exactly one successor per token. Requests inside `ROTATION_GRACE` authenticate from the session without issuing a token; replaying an older rotated token revokes the whole session. Push subscriptions belong to `AuthSession`s and are removed when the session is revoked.
- The three challenge numbers must stay pairwise distinct in **both** digit positions (`LoginChallenge.isClearlyDistinct`).
- Sensitive account mutations carry `@RequiresStepUp`; the frontend's axios interceptor handles the 403 `STEP_UP_REQUIRED` by opening `StepUpModal` and retrying once.
- **Frontend routing / theming:** Before adding pages, you MUST read `appRoutes.tsx`, `utils/routingUtils.ts` and `theme.ts`.
- **Outbound mail:** Before changing templates, providers, or the outbox, you MUST read `mail/config/MailAutoConfig.java`, `mail/outbox/MailOutboxRepository.java`, and the relevant template(s) under `mail/templates/`.