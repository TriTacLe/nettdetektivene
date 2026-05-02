# Nettdetektivene

**NTNU Course Project — IDATT2106: System Development 2 with Agile Project**

A gamified educational web application for pupils in grades 3–7, teaching digital safety, critical thinking about disinformation, and cybercrime awareness through detective-themed tasks. Teachers create classrooms, distribute join codes, and track student progress.

---

## Project Context

This is a student project developed for the course **IDATT2106** at NTNU (Norwegian University of Science and Technology) during Spring 2026. The project follows agile development methodologies with a two-sprint structure.

**Target Audience:**

- Primary users: Students in grades 3–7
- Secondary users: Teachers managing classrooms and tracking progress

**Educational Goals:**

- Develop students' critical thinking skills regarding digital content
- Provide an engaging learning experience about internet safety
- Build resilience against digital crime and desinformation
- Help students recognize and handle fake news, manipulated images, and phishing attempts

**Problem Addressed:**
Over 70% of adults express concerns about information legitimacy, and children are particularly vulnerable due to their developing critical thinking skills. This solution aims to provide early education in digital literacy and safety.

---

## Technology Stack

### Backend

- **Framework:** Spring Boot 3.5 (Java 21)
- **Database:** MySQL 8 with Flyway migrations
- **Security:** JWT authentication with bcrypt password hashing, Spring Security
- **Architecture:** RESTful API following layered architecture (Controller → Service → Repository)
- **Data Pattern:** MySQL stored procedures and triggers for game logic consistency
- **API Documentation:** Springdoc OpenAPI 3 (Swagger UI at `/swagger-ui.html`)

### Frontend

- **Framework:** Vue 3 with Composition API
- **Build Tool:** Vite
- **State Management:** Pinia with persistence
- **Routing:** Vue Router 5
- **Styling:** Tailwind CSS 4
- **UI Icons:** Lucide Vue Next
- **HTTP Client:** Axios (`ApiService`) and fetch-based OpenAPI-generated type-safe client
- **Avatar System:** Dicebear
- **Language:** TypeScript

### DevOps & Infrastructure

- **Containerization:** Docker & Docker Compose
- **CI/CD:** GitHub Actions (GitOps workflow)
- **Production Server:** NTNU OpenStack VM (OrbitNTNU)
- **Container Registry:** GitHub Container Registry (GHCR)
- **Web Server:** Nginx (TLS termination, reverse proxy, SPA routing)
- **Reverse Proxy:** Nginx handling `/api` routes to backend
- **SSL:** Let's Encrypt with certbot
- **Orchestration:** Managed via Makefile

---

## Prerequisites

### Required Software

- **Docker** + **Docker Compose**
- **Bash shell** (for Makefile scripts)
- **Node.js 22** and **Java 21** (only if running services outside Docker)

### Account Requirements

- Access to server VM (production deployment)
- GitHub account (for CI/CD)
- NTNU GitLab account (for code storage and peer review)

---

## Project Structure

```
.
├── Makefile              # Primary entry point for local development
├── compose.yaml          # Development Docker Compose configuration
├── compose.prod.yaml     # Production Docker Compose configuration
├── .env                  # Root environment variables (MySQL + JWT secrets)
├── .env.example          # Template for root environment variables
├── .github/              # GitHub Actions workflows and issue/PR templates
├── scripts/              # Helper shell scripts (issue tooling)
├── backend/
│   ├── Dockerfile        # Multi-stage build for Spring Boot
│   ├── compose.yaml      # Backend-only Docker Compose for standalone dev
│   ├── mvnw              # Maven wrapper script
│   ├── pom.xml           # Maven dependencies and build configuration
│   ├── CLAUDE.md         # Backend implementation guidelines
│   ├── docs/
│   │   └── database/     # Database schema and trigger/procedure documentation
│   └── src/
│       ├── main/
│       │   ├── java/     # Spring Boot application
│       │   │   ├── controller/    # REST endpoints
│       │   │   ├── service/       # Business logic
│       │   │   ├── repository/    # JPA repositories
│       │   │   ├── model/         # JPA entities
│       │   │   ├── dto/           # Request/Response DTOs
│       │   │   ├── mapper/        # Entity ↔ DTO mapping
│       │   │   ├── security/      # JWT authentication filters and utilities
│       │   │   ├── shared/        # Shared utilities (ApiResponse)
│       │   │   ├── exception/     # Global exception handling
│       │   │   └── config/        # Spring configuration
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── db/            # Flyway migration scripts
│       │       └── seed-images/   # Task images bundled for data seeding
│       └── test/java/             # Comprehensive test suite
│           ├── controller/    # @WebMvcTest endpoint tests
│           ├── service/       # Unit tests with Mockito
│           ├── integration/   # Full Spring context tests with H2 in-memory database
│           ├── security/      # JWT filter and utility tests
│           └── util/          # Shared test factories
├── frontend/
│   ├── Dockerfile            # Multi-stage build for Nginx
│   ├── nginx.conf            # Nginx configuration (SPA routing + API proxy)
│   ├── package.json          # Node dependencies
│   ├── vite.config.ts        # Vite build configuration
│   ├── vitest.config.ts      # Vitest unit test configuration
│   ├── cypress.config.ts     # Cypress E2E test configuration
│   ├── openapi-ts.config.ts  # OpenAPI TypeScript client generation config
│   ├── CLAUDE.md             # Frontend implementation guidelines
│   ├── cypress/              # Cypress E2E tests
│   ├── public/               # Static assets served directly (not bundled)
│   └── src/
│       ├── components/        # Reusable Vue components
│       ├── views/             # Page-level components
│       │   └── teacher/       # Teacher-facing views
│       ├── stores/            # Pinia state stores
│       ├── services/          # API service wrappers
│       ├── router/            # Vue Router configuration and navigation guards
│       ├── composables/       # Vue composables (shared reactive logic)
│       ├── types/             # TypeScript type definitions
│       ├── utils/             # Utility functions
│       ├── assets/            # Global stylesheet (main.css)
│       ├── data/              # Static game data (dialogs, stop data, side quests)
│       ├── lib/               # API client configuration and token refresh logic
│       ├── generated-client/  # OpenAPI-generated TypeScript client
│       └── __tests__/         # Frontend unit tests (Vitest)
├── deployment/
│   ├── deploy.sh             # Full deployment script
│   ├── quick-deploy.sh       # Quick re-deploy without rebuild
│   ├── nginx.conf            # Production Nginx configuration (TLS termination)
│   └── nettdetektivene.service  # systemd service unit file
└── README.md                 # This file
```

---

## Quick Start

### 1. Configure Environment Variables

There are **two** required `.env` files:

#### Root `.env` (Docker Compose and Backend)

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
# Database configuration
MYSQL_USER=nettdetektivene
MYSQL_PASSWORD=<strong password>
MYSQL_DATABASE=nettdetektivene
MYSQL_ROOT_PASSWORD=<strong root password>

# JWT configuration
JWT_SECRET=<generate with: openssl rand -base64 32>
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
```

**Security Note:** The `MYSQL_ROOT_PASSWORD` is used by MySQL healthchecks, so it cannot be empty.

#### Frontend `.env` (Vite build-time variables)

```bash
cp frontend/.env.example frontend/.env
```

Defaults are suitable for local development:

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=Nettdetektivene
```

In Docker `prod` mode, the frontend Nginx proxies `/api` to the backend, so `VITE_API_URL` is set to `/api/v1` (relative) via `frontend/.env.production`.

---

### 2. Running the Application

The `Makefile` is the recommended entry point for all operations.

| Command              | Description                                                                                                  |
| -------------------- | ------------------------------------------------------------------------------------------------------------ |
| `make dev`           | Builds and starts the full stack (MySQL + backend + frontend), waits for backend and frontend, tails logs    |
| `make prod`          | Same services as `dev`, with the production frontend image (nginx); waits for backend, prints recent logs    |
| `make status`        | Shows listeners on ports 8080, 3307, 5173 and the running Docker containers                                  |
| `make logs`          | Tails logs for a service: `make logs backend` (also accepts `frontend`, `mysql`)                             |
| `make down`          | Stops containers, preserves volumes                                                                          |
| `make clean`         | Stops containers **and removes project images + named volumes** (complete reset)                             |
| `make test`          | Runs `test-backend` followed by `test-frontend`                                                              |
| `make test-backend`  | Runs backend tests (`./mvnw clean test -q`) — JUnit + Mockito + H2                                           |
| `make test-frontend` | Runs frontend unit tests (`npm run test:unit -- --run`) — Vitest                                             |
| `make test-e2e`      | Runs Cypress E2E tests (requires the stack to be up — `make dev` first)                                      |
| `make test-coverage` | Runs `./mvnw clean verify -q` to produce the JaCoCo coverage report                                          |

> The dev and prod variants currently start the same services. `compose.yaml` enables the `seed` Spring profile so first-startup data seeding fires automatically.

#### Service URLs Once Running:

- **Backend:** `http://localhost:8080` (Swagger UI: `/swagger-ui.html`, OpenAPI spec: `/api-docs`)
- **Frontend:** `http://localhost:5173` — served by the dockerized nginx container started by `make dev` / `make prod`
- **MySQL:** `localhost:3307` (mapped from container `:3306`)

For frontend hot-module reload during UI work, run a Vite dev server alongside the dockerized stack:

```bash
cd frontend && npm install && npm run dev
```

Vite picks an available port (typically 5174 if 5173 is taken by the docker frontend) and proxies API calls per `VITE_API_URL`.

---

### 3. Database Seeding

Seeding is driven by an idempotent Java component, `DataSeeder` (`backend/src/main/java/ntnu/no/backend/config/DataSeeder.java`), gated by the Spring `seed` profile. It creates institutions, users, classrooms, tasks (with real seed images copied into `uploads/tasks/seed/`), achievements, notebook entries, and sample mystery submissions.

**Automatic Seeding:**
`make dev` runs the backend with `SPRING_PROFILES_ACTIVE=dev,seed` (set in `compose.yaml`), so seeding fires on the first healthy startup. Re-running is safe — every step uses `findBy*` lookups before inserting.

**Manual Seeding:**
To run the seeder against a local DB outside Docker:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

**What Gets Created:**

- 4 institutions: Flatåsen skole, Byåsen skole, Nanset skole, Skrinhagen skole
- 5 instructors: `laerer@flataasen.no` (Nora), `laerer2@flataasen.no` (Regine), `laerer@byaasen.no` (Lars), `laerer@nanset.no` (Gustav), `laerer@skrinhagen.no` (Silje)
- 10 classrooms across grades 3–7 (e.g. 3A, 3B, 4A … 7B), each with a unique join code printed to the backend log on startup
- Students enrolled across the classrooms — `student1..5@ntnu.no` are clean fixtures with no progress; `tester@ntnu.no` is the demo user with all tasks completed
- 11 task definitions, attached to the seeded classrooms:
  - 3 NEWS tasks at Nyhetskvartalet (difficulties 1–3)
  - 3 PHOTO tasks at Fotografen (difficulties 1–3)
  - 5 mixed tasks at Datasenteret (3 NEWS + 2 PHOTO at difficulties 4–5)
- Mystery submissions in mixed PENDING / APPROVED / REJECTED states for demo
- Achievements (incl. the secret `caught_hacker` and `quiz_perfect`)
- Notebook entries on the tester user

**Default Credentials:**
All accounts use password: `password123`

**Resetting Database:**
To start fresh:

```bash
make clean
make dev
```

---

## Development Workflow

### Frontend Development Server (HMR)

For hot module replacement during frontend development:

```bash
cd frontend
npm install
npm run dev
```

This runs the Vite dev server on port 5173 with HMR enabled.

### Backend Development (Outside Docker)

For local backend development with hot reload:

```bash
cd backend
./mvnw spring-boot:run
```

### Testing

```bash
# Run all tests
make test

# Backend only
make test-backend

# Frontend only
make test-frontend

# Coverage report
make test-coverage  # View at: backend/target/site/jacoco/index.html

# E2E tests (requires running stack)
make test-e2e
```

---

## Course Requirements (IDATT2106)

### Testing Coverage Requirements

- **Backend:** Minimum 50% code coverage (measured by JaCoCo)
- **Frontend:** Minimum 30% code coverage (measured by Vitest)
- **Test Types Required:**
  - Unit tests (service logic, isolated)
  - Integration tests (H2 in-memory database)
  - E2E tests (Cypress)
  - Unit tests emphasized for frontend

### Required Technologies

✓ **Docker & Docker Compose** for containerization
✓ **Spring Boot 3.5** (Java 21) for backend
✓ **Vue 3** or equivalent for frontend SPA
✓ **MySQL** for persistence (or in-memory H2 for testing)
✓ **Git** for version control (NTNU GitLab + GitHub for CI/CD)
✓ **Maven** as build system for backend
✓ **REST API** architecture
✓ **Continuous Integration** (GitHub Actions)
✓ **Build systems** that run tests automatically

### Architectural Requirements

- **Layered Architecture:** Controller → Service → Repository pattern
- **RESTful API:** Proper HTTP verbs, status codes, and hierarchical URIs
- **Security:**
  - Authentication and authorization
  - Password hashing
  - Protection against OWASP A03:2021-Injection (SQL injection, XSS)
  - JWT-based stateless authentication
- **WCAG 2.1 Compliance:** Web Content Accessibility Guidelines
- **Responsive Design:** Support for Chromium (Chrome) and Firefox browsers

### Database Requirements

- All data must be stored in MySQL database
- Database functionality exposed via REST services
- Transactions and data integrity enforced through:
  - MySQL stored procedures
  - Triggers for automatic consistency
  - Optimistic locking where appropriate

---

## API Documentation

Once the backend is running, comprehensive API documentation is available at:

**Swagger UI:** http://localhost:8080/swagger-ui.html

OpenAPI specification: http://localhost:8080/api-docs

The frontend uses **OpenAPI TypeScript code generation** for type-safe API client usage.

---

## Database Schema

The database is organized into logical groups:

### Core Tables

- `app_user` — User accounts
- `app_user_local_credential` — Password authentication
- `app_user_refresh_token` — JWT refresh tokens
- `app_user_identity` — Third-party identity providers (Feide support)

### Authorization

- `role` — System roles (student, instructor, admin)
- `user_role` — User-role assignments

### Profile & Institutions

- `user_profile` — Game-specific profile data (XP, level, avatar config)
- `institution` — Schools/institutions (e.g., Flatåsen skole)

### Classrooms

- `classroom` — Classroom instances
- `classroom_instructor` — Teacher-to-classroom assignments
- `classroom_student` — Student-to-classroom memberships (with display name approval flow)
- `classroom_task` — Task assignments to classrooms

### Tasks & Progress

- `task` — Task definitions (built-in and teacher-created)
- `task_attempt` — Full history of all attempts
- `task_best_score` — Best result per task (managed by triggers, read-only in Java)

### Achievements

- `achievement` — Achievement definitions
- `user_achievement` — Achievement awards

### Game Features

- `notebook_entry` — Student notes and reflections
- `mystery_submission` — Weekly mystery submissions (pending/approved/rejected)
- `mystery_completion` — Records which student solved which approved mystery (composite PK on user + submission)
- `notification` — Notifications to users (especially instructors)
- `user_task_unlocked` — Tracks which tasks each student has unlocked in a classroom

**Key Design Pattern:** Tasks follow a progressive difficulty system across game stops (Nyhetskvartalet, Fotografen, Datasenteret).

---

## MySQL Triggers & Stored Procedures

The database uses triggers and stored procedures for automatic consistency:

### Triggers

- `trg_classroom_auto_instructor` — Auto-assigns classroom creator as instructor
- `trg_attempt_sync` — Updates `task_best_score`, awards XP, calculates levels on task completion
- `trg_mystery_approved` — Creates a notification when a mystery submission is approved
- `trg_mystery_completion` — Awards the achievement to the solver when a `mystery_completion` row is inserted
- `trg_notify_attempt_failed` — Notifies instructors when students fail tasks
- `trg_notify_stop_completed_insert/update` — Notifies instructors when students complete a stop location

### Stored Procedures

- `sp_submit_attempt` — Single entry point for task submissions with validation
- `sp_request_display_name_change` — Student requests display name change
- `sp_review_display_name_change` — Teacher approves/rejects name changes

**Important:** `task_best_score` is managed entirely by triggers —never call `.save()` on this entity in Java.

---

## CI/CD Pipeline

The project uses a **GitOps** workflow with GitHub Actions automation on the OrbitNTNU repository.

### Git Remotes

- **origin**: NTNU GitLab — Code storage, peer review, pull requests
- **orbit**: GitHub/OrbitNTNU — CI/CD trigger (managed by project lead)

### Pipeline Flow

1. Developer pushes feature branch to both `origin` and `orbit`
2. Pull requests and merges happen on NTNU GitLab only
3. Project lead merges to deploy staging branch (`dev-prod` or `prod-test`)
4. Push to `orbit` branch triggers GitHub Actions
5. CI builds and tests code, creates Docker images
6. Images pushed to GitHub Container Registry (GHCR)
7. Deploy job SSHs into production server, pulls images, restarts containers

### Stages

1. **Build & Test:**
   - Maven multi-stage Docker build for backend
   - Node.js multi-stage Docker build for frontend
   - Run all tests (backend + frontend)
   - Generate coverage reports

2. **Push to Registry:**
   - Immutable images tagged with commit SHA
   - Mutable `:latest` tags for deployment

3. **Deploy:**
   - SSH into production server (OrbitNTNU VM)
   - Docker Compose pulls new images
   - Restart only changed services

---

## Production Deployment

### Live Application

**URL:** https://nettdetektivene.orbitntnu.com/

### Production Server

- **Infrastructure:** NTNU OpenStack VM
- **OS:** Linux
- **Location:** OrbitNTNU project infrastructure
- **Access:** SSH only (OrbitNTNU members only)

### Production Stack

- **Host-level Nginx:** TLS termination, SSL (Let's Encrypt certbot)
- **Docker Compose:** Orchestrates all services
- **Services:**
  - nd-db: MySQL 8.0
  - nd-backend: Spring Boot application
  - nd-frontend: Nginx serving Vue SPA
- **Volumes:** Persistent MySQL data, development uploads

### Security Measures

- Network isolation (MySQL and backend on private Docker bridge network)
- JWT-based stateless authentication
- Role-based access control (`@PreAuthorize` annotations)
- Minimal container images (no build tools at runtime)
- SSL/TLS for all external traffic
- Security headers (X-Frame-Options, X-Content-Type-Options, X-XSS-Protection)
- Secrets management via environment variables (never in code or containers)

### Monitoring

- Docker container health checks
- Application logs via `docker compose logs`
- System logs via system and application monitoring
- SSL certificate renewal (certbot auto-renewal)

---

## Code Quality & Development Guidelines

### Backend Guidelines

- **Layered Architecture:** Controller → Service → Repository
- **DTO Pattern:** Never return JPA entities from controllers — always map to DTOs
- **Validation:** `@Valid` on all request DTOs
- **Security:**
  - `@PreAuthorize` on all data-modifying endpoints
  - Fetch Lazy loading on relationships
- **Triggers:** Never reimplement trigger logic in Java
- **Read-only Entities:** `task_best_score` is read-only
- **Exception Handling:** GlobalExceptionHandler prevents stack traces from leaking

### Frontend Guidelines

- **No Direct Axios in Components:** Use Pinia stores and API services
- **State Management:** Pinia with sessionStorage for durability
- **Route Protection:** Navigation guards in Vue Router
- **Error Handling:** User-friendly error messages (no raw errors)
- **Responsive Design:** Mobile and tablet support
- **Accessibility:** WCAG 2.1 compliance
- **TypeScript:** Strong typing throughout the application

### Naming Conventions

**Backend:**

- Entities: PascalCase matching database snake_case
- DTOs: `XxxRequestDTO`, `XxxResponseDTO`
- Classes: PascalCase
- Methods/Variables: camelCase
- Branches: `feature/<issue-number>-<short-desc>`

**Frontend:**

- Components: PascalCase
- Views: PascalCase + `View` suffix
- Files (components, stores, services): PascalCase
- Variables/Functions: camelCase
- Types/Interfaces: PascalCase

### Testing Requirements

- **Backend:** JUnit 5 + Mockito + H2
- **Frontend:** Vitest + Vue Test Utils + Cypress
- **Coverage:** 50% backend, 30% frontend minimum
- **Test Factory Pattern:** Shared test data builders
- Realistic Test Data: Use realistic names, not placeholders

---

## Team and Course Information

**Course:** IDATT2106 — System Development 2 with Agile Project
**Semester:** Spring 2026
**Credits:** 7.5 ECTS
**Institution:** NTNU (Norwegian University of Science and Technology)
**Department:** Institute of Computer Science and Informatics (IDI)

**Faculty:**

- **Grethe Sandstrak** (Course Coordinator)
- **Muhammad Ali Norozi** (Lecturer)
- **Surya Bahadur Kathayat** (Lecturer)

**Team Members:** See project dashboard for current team roster

**Scrum Process:**

- Daily standups at fixed time
- Two sprint structure
- Agile development with product owner management
- Continuous integration and tests
- Peer review via pull requests

---

## Troubleshooting

### Common Issues

**Backend Won't Start:**

```bash
# Check logs
make logs backend

# Verify MySQL is healthy
docker ps

# Try full reset
make clean
make dev
```

**Frontend Cannot Reach Backend:**

- Verify backend is running: `curl http://localhost:8080/swagger-ui.html` (the project does not expose `/health`; `make dev` instead waits for `http://localhost:8080` to respond)
- Check `VITE_API_URL` in `frontend/.env`
- Verify proxy configuration in `vite.config.ts`

**Database Connection Issues:**

```bash
# Check MySQL logs
make logs mysql

# Verify environment variables
cat .env | grep MYSQL

# Test MySQL connection
docker exec -it nd-db mysql -u root -p
```

**Tests Failing:**

```bash
# Run tests with verbose output
cd backend && ./mvnw clean test
cd frontend && npm run test:unit -- --reporter=verbose

# Check test configuration
cat backend/src/test/resources/application-test.properties
```

**Port Conflicts:**

```bash
# Check what's using the ports
make status

# Kill processes if needed
lsof -ti:8080 | xargs kill -9
lsof -ti:3307 | xargs kill -9
lsof -ti:5173 | xargs kill -9
```

---

## Additional Documentation

- **Backend Implementation Plan:** `backend/CLAUDE.md`
- **Frontend Implementation Plan:** `frontend/CLAUDE.md`

- **Database Schema:** `backend/docs/database/tables.md`
- **Triggers & Procedures:** `backend/docs/database/triggers-and-procedures.md`

- **API Usage:** `frontend/docs/API_CLIENT_GUIDE.md`

---

## License

This project is developed for educational purposes at NTNU.

---

## Acknowledgments

This project is developed for the course **IDATT2106** at the Norwegian University of Science and Technology (NTNU). Special thanks to the product owners and faculty for guidance throughout the development process.

---

**Last Updated:** May 2026
**Version:** v26-05

