# CLAUDE.md — Backend (nettdetektivene)

## Project Overview
Spring Boot REST API backend for the nettdetektivene educational app.
Base URL: `localhost:8080/api/v1/`

**Tech stack:** Java · Spring Boot · Maven · MySQL · Flyway · JWT · Springdoc OpenAPI

---

## Architecture

### Package Structure
```
src/main/java/...
├── controller/
├── service/
├── repository/
├── entity/          # JPA entities (CamelCase matching db snake_case)
├── dto/
│   ├── request/     # XxxRequestDTO
│   └── response/    # XxxResponseDTO
├── security/        # JWT only — do NOT touch if you're not Nora (A)
├── shared/          # ApiResponse<T>
├── exception/       # GlobalExceptionHandler + domain exceptions
└── config/
```

### Naming Conventions
| Thing | Convention | Example |
|---|---|---|
| DB table | snake_case | `app_user` |
| Entity class | PascalCase | `AppUser` |
| DTO | `XxxRequestDTO` / `XxxResponseDTO` | `LoginRequestDTO` |
| Controller | `XxxController` | `AuthController` |
| Service | `XxxService` | `UserService` |
| Branch | `feature/<issue-number>-<short-desc>` | `feature/7-auth-jwt` |

---

## Dependency Layers

Build in this order — never reference a layer above before the one below is done:

| Layer | Depends on | Tables / Entities |
|---|---|---|
| 0 | — | `role`, `app_user`, `institution`, `achievement` |
| 1 | Layer 0 | `app_user_local_credential`, `app_user_refresh_token`, `app_user_identity`, `user_role`, `user_profile`, `classroom` |
| 2 | Layer 1 | `task`, `classroom_instructor`, `classroom_student`, `classroom_task` |
| 3 | Layer 2 | `task_best_score`, `user_achievement`, `notebook_entry`, `mystery_submission`, `notification` |

---

## Build Order (Steps)

### Step 0 — Shared config (do FIRST, together)
- `application.properties`, `pom.xml`, `.env` — agree and commit before anything else
- Write `ApiResponse<T>` → `shared/`
- Write `GlobalExceptionHandler` → `exception/`
- **Commit and push before any controller is written**

### Step 1 — Entities & Repositories
- Start with Layer 0, commit immediately so the rest of the team can unblock
- All repos extend `JpaRepository` — Spring implements them automatically
- No service logic here, no risk of circular deps

### Step 2 — Auth (JWT + Security)
- `JwtUtil` — generates and validates access tokens
- `JwtAuthFilter` — `OncePerRequestFilter`, reads user from header
- `SecurityConfig` — permits `/api/v1/auth/**`, locks everything else, configures CORS/CSRF

### Step 3 — Services (repo deps only)
- `UserService`, `RoleService`, `InstitutionService`, `AchievementService`

### Step 4 — Services (service deps)
In this order:
1. `UserProfileService` ← `UserService`
2. `ClassroomService` ← `UserService`, `InstitutionService`, `UserProfileService`
3. `ClassroomMemberService` ← `ClassroomService`, `UserService`
4. `TaskService` ← `UserService`
5. `TaskAttemptService` ← `TaskService`, `ClassroomService`
6. `NotebookService` ← `UserService`, `ClassroomService`
7. `MysteryService` ← `ClassroomService`, `UserService`, `AchievementService`
8. `NotificationService` ← `UserService`, `ClassroomService`

### Step 5 — Controllers / Endpoints

### Step 6 — Tests (required before push)

---

## Team Responsibilities

| Person | Areas |
|---|---|
| **A — Nora** | Auth, Roles, Profile, Classroom |
| **B — Tri** | Tasks, Progress, Achievements, Notebook, Mystery, Notification, Institution |

- Code against the **entity/repository interface**, not against services — A and B don't block each other
- **A owns `security/`** — B never touches those files
- Shared files (`ApiResponse`, `GlobalExceptionHandler`, `pom.xml`) — agree verbally before changing
- Code review: A reviews B's PRs and vice versa

---

## Database Rules

### ⚠️ MySQL Triggers — Do NOT re-implement in Java

| Trigger / Procedure | When | What it does |
|---|---|---|
| `trg_classroom_auto_instructor` | AFTER INSERT classroom | Auto-inserts creator as instructor — do NOT do this in `ClassroomService` |
| `trg_attempt_sync` | AFTER INSERT task_attempt | Upserts `task_best_score`, awards 10 XP on first pass, updates `xp_level` |
| `trg_mystery_approved` | AFTER UPDATE mystery_submission | Creates notification + awards achievement on approval (idempotent via INSERT IGNORE) |
| `sp_submit_attempt` | Stored procedure | **Only** entry point for registering attempts — call via `JdbcTemplate`, then read back state |

### `task_best_score`
- **Read-only in Java.** Updated exclusively by `trg_attempt_sync`.
- Never call `.save()` on this table. Treat it as a denormalized cache.

### Soft Delete
- All repository queries against `classroom` must filter on `deletedAt IS NULL`.

---

## Coding Rules

- **Never return JPA entities from controllers** — always map to DTO
- `@PreAuthorize` on every endpoint that modifies classroom data
- `@Enumerated(EnumType.STRING)` on all ENUM columns — values must match MySQL ENUM exactly
- UUID generated in Java: `UUID.randomUUID().toString()` — not by the database
- No wildcard imports (`import lombok.*` etc.)
- Input validation with `@Valid` + Bean Validation on all request DTOs
- Use `springdoc-openapi` from day one — frontend can test on `/swagger-ui.html`
- Seed data: roles `STUDENT` and `TEACHER` must be in a Flyway migration from day one

---

## Config & Secrets

**`application.properties`**
```properties
spring.application.name=nettdetektivene
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS}
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
server.port=8080
```

**`.env`** (never commit — add to `.gitignore`)
```env
DB_URL=jdbc:mysql://localhost:3306/<dbname>
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=   # generate: openssl rand -base64 32
JWT_EXPIRATION_MS=86400000
```

Commit `.env.example` as a template for the team.

> **Note:** Two `.env` files and two `docker-compose` files exist — confirm which applies to backend vs frontend.

---

## Testing

- **Unit tests:** `JUnit 5` + `Mockito` — service logic isolated from DB
- **Integration tests:** `Testcontainers` with MySQL image (required for triggers/stored procedures — H2 cannot run these)
- **Controller tests:** `@WebMvcTest` + `MockMvc` — verify HTTP status codes and JSON structure
- Integration tests go under `test/.../integration/` (slower, run separately)
- **Do NOT use H2**
- Rule: test logic you wrote yourself. Entities and DTOs don't need direct unit tests — they're covered by integration tests
- **All tests must be green before pushing**

---

## Shared Utilities

### `ApiResponse<T>` — `shared/`
Wrapper that ensures all endpoints return the same JSON structure. Used in every controller. `ResponseEntity` controls the HTTP layer; `ApiResponse<T>` controls the JSON body.

### `GlobalExceptionHandler` — `exception/`
Catches all unhandled exceptions and returns them as `ApiResponse` instead of Spring's raw stack trace. Define separate exception classes for 400, 401, 403, 404, 500.


## Pull Request and commit messages guidelines
- No credits to Claude
- Give simple template
- Don't assume, if you have questions ask
- No emojis silly. 
- No 
overcomplicated explainations