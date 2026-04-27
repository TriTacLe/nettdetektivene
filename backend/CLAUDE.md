# Backend Implementation Plan

This repo uses git.ntnu.no which is github

## 0. Rules for Claude

- Never edit files, commit, or push unless the user explicitly asks.
- Only explain and guide unless told to make changes.
## 1. Dependency Layers

| Layer | Depends on | Tables / Entities                                                                                                    |
| ----- | ---------- | -------------------------------------------------------------------------------------------------------------------- |
| 0     | —          | `role`, `app_user`, `institution`, `achievement`                                                                     |
| 1     | Layer 0    | `app_user_local_credential`, `app_user_refresh_token`, `app_user_identity`, `user_role`, `user_profile`, `classroom` |
| 2     | Layer 1    | `task`, `classroom_instructor`, `classroom_student`, `classroom_task`                                                |
| 3     | Layer 2    | `task_best_score`, `user_achievement`, `notebook_entry`, `mystery_submission`, `notification`                        |

Never start working on X if Y that X depends on is not implemented yet.

---

## 2. Team Responsibilities

| Person          | Areas                                                                                                                                |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| A — Nora, Anine | Auth, user_profile, classroom, institution, classroom_instructor, classroom_student                                                  |
| B — Tri         | Achievement, task, classroom_task, task_attempt, task_best_score, user_achievement, notebook_entry, mystery_submission, notification |

Code against the entity and repository interface, not against services — A and B won't block each other.

---

## 3. File Structure and Naming

```
src/main/java/...
├── controller/
├── service/
├── repository/
├── model/           # JPA entities (PascalCase matching db snake_case)
├── dto/
│   ├── XxxRequestDTO
│   └── XxxResponseDTO
├── mapper/
├── security/        # JWT only — do NOT touch if you are not Nora (A)
├── shared/          # ApiResponse<T>
├── exception/       # GlobalExceptionHandler + domain exceptions
└── config/
```

**Naming conventions:**

| Type       | Convention                            | Example              |
| ---------- | ------------------------------------- | -------------------- |
| DTO        | `XxxRequestDTO` / `XxxResponseDTO`    | `LoginRequestDTO`    |
| Controller | `XxxController`                       | `AuthController`     |
| Service    | `XxxService`                          | `UserService`        |
| Entity     | PascalCase matching db snake_case     | `AppUser`            |
| Branch     | `feature/<issue-number>-<short-desc>` | `feature/7-auth-jwt` |

---

## 4. Build Order

### Step 0 — Shared config (do FIRST, together)

Agree on `application.properties`, `pom.xml`, `.env` and commit before anything else.

- Write `ApiResponse<T>` → `shared/`
- Write `GlobalExceptionHandler` → `exception/`
- Commit and push before any controller is written

### Step 1 — Entities and Repositories

Start with Layer 0, commit immediately so the rest of the team can unblock.
All repos extend `JpaRepository` — Spring implements them automatically.

### Step 2 — Auth (JWT + Security)

- `JwtUtil` — generates and validates access tokens
- `JwtAuthFilter` — `OncePerRequestFilter`, reads user from header
- `SecurityConfig` — permits `/api/v1/auth/**`, locks everything else, configures CORS/CSRF

### Step 3 — DTOs and Mappers

Not blocked by anything — can be done at any time.

### Step 4 — Services (repo dependencies only)

- `UserService`, `RoleService`, `InstitutionService`, `AchievementService`

Then in this order (service dependencies):

1. `UserProfileService` ← `UserService`
2. `ClassroomService` ← `UserService`, `InstitutionService`, `UserProfileService`
3. `ClassroomMemberService` ← `ClassroomService`, `UserService`
4. `TaskService` ← `UserService`
5. `TaskAttemptService` ← `TaskService`, `ClassroomService`
6. `NotebookService` ← `UserService`, `ClassroomService`
7. `MysteryService` ← `ClassroomService`, `UserService`, `AchievementService`
8. `NotificationService` ← `UserService`, `ClassroomService`

### Step 5 — Controllers / Endpoints

Base URL: `localhost:8080/api/v1/`

### Step 6 — Tests (required before push)

See testing section below.

---

## 5. MySQL Triggers — Do NOT re-implement in Java

| Trigger / Procedure              | When                            | What it does                                                                              |
| -------------------------------- | ------------------------------- | ----------------------------------------------------------------------------------------- |
| `trg_classroom_auto_instructor`  | AFTER INSERT classroom          | Auto-inserts creator as instructor — do NOT do this in `ClassroomService`                 |
| `trg_attempt_sync`               | AFTER INSERT task_attempt       | Upserts `task_best_score`, awards 10 XP on first pass, updates `xp_level`                 |
| `trg_mystery_approved`           | AFTER UPDATE mystery_submission | Creates notification + awards achievement on approval (idempotent via INSERT IGNORE)      |
| `trg_notify_attempt_failed`      | AFTER INSERT task_attempt       | Notifies all classroom instructors when a student fails a task attempt                    |
| `sp_submit_attempt`              | Stored procedure                | Only entry point for registering attempts — call via `JdbcTemplate`, then read back state |
| `sp_request_display_name_change` | Stored procedure                | Student requests a display name change, creates pending review                            |
| `sp_review_display_name_change`  | Stored procedure                | Teacher approves/rejects display name change request                                      |

`task_best_score` is read-only in Java — never call `.save()` on it.

## 6. Test Coverage (Jacoco)

Coverage is measured by Jacoco plugin on `mvn verify`. The plugin is configured in `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Generate report:

```bash
./mvnw clean verify
# Report location: target/site/jacoco/index.html
```

**Note:** `BackendApplicationTests` is `@Disabled` — do not try to fix it, it is intentionally disabled.

---

## 7. Testing

### Test Types

| Type        | Tools                   | What is tested                       |
| ----------- | ----------------------- | ------------------------------------ |
| Unit        | JUnit 5 + Mockito       | Service logic isolated from DB       |
| Controller  | `@WebMvcTest` + MockMvc | HTTP status codes and JSON structure |
| Integration | H2 in-memory database   | Repository and service integration   |

### File Structure

```
src/test/java/.../
  controller/     # @WebMvcTest tests
  service/        # Unit tests with Mockito
  integration/    # Testcontainers tests (slower, run separately)
  util/           # TestFactory classes (shared test data)
```

### Test Factories

Use factories in `util/` to build test data. Never repeat `Task.builder()...` across multiple test files.

- Create a factory when the same object is built in 2+ test files
- Factory has static methods: `entity()`, `requestDTO()`, `responseDTO(UUID id)`
- Examples: `TaskTestFactory`, `ClassroomTestFactory`, `AchievementTestFactory`

**Test data naming — use realistic names, not placeholders:**

| Type | Use | Avoid |
|------|-----|-------|
| Student display names | `"Tri"`, `"Nora"`, `"Anine"` | `"Alice"`, `"Bob"`, `"Test User"` |
| Task titles | `"Spot the phishing email"`, `"Identify the suspicious packet"` | `"Task 1"`, `"Test Task"` |
| Classroom titles | `"Cybersecurity 101"` | `"Test Classroom"` |
| Emails | `"tri@ntnu.no"`, `"nora@ntnu.no"` | `"test@example.com"` |

### Service Tests (Unit)

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
  @Mock MyRepository myRepository;
  @InjectMocks MyService myService;
}
```

**Test per method:**

- Happy path returns correct DTO
- `EntityNotFoundException` is thrown when not found
- `verify(repo).save(any(...))` when saving
- Soft delete sets `deletedAt` — don't just verify `deleteById` is called

### Controller Tests (@WebMvcTest)

```java
@WebMvcTest(MyController.class)
class MyControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockitoBean MyService myService;
}
```

**Rules:**

- `@WithMockUser` on all authenticated tests
- `@WithMockUser(roles = "instructor")` for role-restricted endpoints
- `.with(csrf())` on all POST, PATCH, DELETE requests
- Wrong role → expect `403 Forbidden`
- No auth → expect `401 Unauthorized`
- Always assert `$.success` and at least one data field in JSON

### Integration Tests (H2 Database)

**Use H2 in-memory database for all tests.**
H2 is used with MySQL compatibility mode to support similar SQL syntax.

Test configuration is in `src/test/resources/application-test.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.flyway.enabled=false
```

**Note:** H2 does NOT support MySQL stored procedures and triggers. These are tested manually or verified through:
- Unit tests of Java code that calls stored procedures
- Manual integration testing against real MySQL
- CI pipeline validation

Run tests with H2 profile:
```bash
./mvnw clean test -Dspring.profiles.active=test
```

### Naming Convention

```
methodName_expectedResult_whenCondition

getTask_returnsDTO_whenFound
getTask_throwsEntityNotFoundException_whenNotFound
createTask_savesAndReturnsDTO
deleteClassroom_softDeletes_whenFound
```

### What to Test

- All service logic you wrote yourself
- HTTP status codes and JSON structure for all endpoints
- Role checks (`@PreAuthorize`)
- Exception handling (not found, forbidden etc.)
- Soft delete sets `deletedAt`

### What NOT to Test

- Entities and DTOs directly
- Spring auto-generated repository methods
- Mapper classes directly (covered by service tests)
- Auto-generated Lombok code

### Rules

- All tests must be green before pushing
- One assertion focus per `@Test` — don't test 5 things in one test
- Tests must be independent — no shared mutable state between tests
- H2 is used for all automated tests (fast, no Docker required)
- Manual testing against MySQL Docker container for stored procedure/trigger validation

---

## 8. Team Collaboration

- A owns `security/` — B never touches those files
- Shared files (`ApiResponse`, `GlobalExceptionHandler`, `pom.xml`) — agree verbally before changing
- Code review: A reviews B's PRs and vice versa

---

## 9. Config and Secrets

**`application.properties`**

```properties
spring.application.name=nettdetektivene
spring.config.import=optional:file:.env[.properties]
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
jwt.refresh-expiration-ms=${JWT_REFRESH_EXPIRATION_MS}
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
JWT_REFRESH_EXPIRATION_MS=604800000
```

Commit `.env.example` as a template for the team.

---

## 9. Coding Rules

**Must do:**

- Always map to DTO before returning from controller — never return JPA entities
- `@PreAuthorize` on every endpoint that modifies data
- `@Valid` on all request DTOs in controllers
- `@Enumerated(EnumType.STRING)` on all ENUM columns — values must match MySQL ENUM exactly
- UUID generated in Java: `UUID.randomUUID()` — not by the database
- Use `FetchType.LAZY` on all `@ManyToOne` and `@OneToMany` relations
- Use `springdoc-openapi` from day one — frontend tests on `/swagger-ui.html`
- `ApiResponse<T>` in every controller — `ResponseEntity` controls HTTP layer, `ApiResponse<T>` controls JSON body
- `GlobalExceptionHandler` in `exception/` — without it Spring leaks stack traces to frontend
- All repos extend `JpaRepository` — Spring implements them automatically
- Soft delete: all repository queries against `classroom` must filter on `deletedAt IS NULL`

**Must not do:**

- Never call `.save()` on `task_best_score` — trigger managed, read-only
- Never re-implement trigger logic in Java (`trg_attempt_sync`, `trg_mystery_approved`, `trg_classroom_auto_instructor`)
- Never award achievements manually in service code — `trg_mystery_approved` handles it
- Never create notifications manually — `trg_mystery_approved` handles it
- Never touch `security/` — owned by Nora
- Never use wildcard imports (`import lombok.*`)
- Never return raw stack traces — `GlobalExceptionHandler` handles all exceptions

---

## 10. Pull Request and Commit Guidelines

- Keep commit messages simple and descriptive
- Don't assume — ask if unsure
- No emojis
- No overcomplicated explanations
- No credits to claude
