# Nettdetektivene

A gamified educational web app where pupils (grades 3–7) learn about digital
safety, fake news, image manipulation, and cybercrime by playing through
detective-themed tasks. Teachers create classrooms, hand out join codes, and
track student progress.

- **Backend** — Spring Boot 3.5 (Java 21) + MySQL 8 + Flyway
- **Frontend** — Vue 3 + Vite + Tailwind 4 + Pinia
- **Container orchestration** — Docker Compose, driven by a top-level Makefile

---

## Prerequisites

- Docker + Docker Compose
- `bash`, `curl`, `jq` (for the seed script)
- Node 22 and Java 21 only if you want to run a service outside Docker

---

## 1. Configure environment

There are **two** `.env` files. Both must be filled in before anything works.

### Root `.env` (used by `docker-compose` and the backend container)

```bash
cp .env.example .env
```

Then edit `.env` and set:

```env
MYSQL_USER=nd-usr
MYSQL_PASSWORD=<pick one>
MYSQL_DATABASE=nd-db
MYSQL_ROOT_PASSWORD=<pick one>
JWT_SECRET=<run: openssl rand -base64 32>
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
```

The healthcheck for the `mysql` service uses `MYSQL_ROOT_PASSWORD`, so it must
not be empty.

### Frontend `.env` (read by Vite at build/dev time)

```bash
cp frontend/.env.example frontend/.env
```

Defaults are fine for local development:

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=Nettdetektivene
```

In Docker `prod` mode the frontend nginx proxies `/api` to the backend, so
`VITE_API_URL` can stay as `http://localhost:8080/api/v1` regardless.

---

## 2. Run the stack

The `Makefile` is the supported entry point.

| Command       | What it does                                                                |
| ------------- | --------------------------------------------------------------------------- |
| `make dev`    | Builds + starts `mysql` and `backend`, waits until backend is healthy, runs `scripts/factory.sh` to seed test data, then tails logs. |
| `make prod`   | Builds + starts `mysql`, `backend`, and `frontend` (nginx). Frontend at `http://localhost:5173`. |
| `make status` | Shows port usage (8080, 3307, 5173) and running containers.                  |
| `make logs`   | Tails logs for any service: `make logs backend`.                            |
| `make down`   | `docker compose down` — stops containers, keeps volumes.                    |
| `make clean`  | `docker compose down -v` — stops containers **and removes the database volume**. Use this to reset. |

Service URLs once running:

- Backend: `http://localhost:8080` (Swagger at `/swagger-ui.html`)
- Frontend: `http://localhost:5173` (only in `prod` mode; for `dev`, run the
  frontend separately with `cd frontend && npm install && npm run dev`)
- MySQL: `localhost:3307` (mapped from container `:3306`)

---

## 3. Populate the database

Seeding is driven by a single idempotent script: `scripts/factory.sh`.

`make dev` runs it automatically once the backend is healthy. To re-seed
manually (after a code change, fresh volume, etc.):

```bash
bash scripts/factory.sh
```

It creates:

- The **NTNU** institution
- One default teacher: `laerer@ntnu.no` (matches the default in the teacher
  login form). Plus `instructor@test.no` and `admin@test.no` for parity with
  older scripts.
- Five students: `student1@test.no` … `student5@test.no`
- A classroom **Testklasse 1** with all students joined and approved
- 11 tasks linked to the classroom: 3 NEWS (Nyhetskvartalet), 3 PHOTO
  (Fotografen), and 5 mixed NEWS/PHOTO at Datasenteret (the boss area)

All default passwords are `password123`. The script is safe to re-run; existing
records are detected by name/email and skipped.

**Resetting from scratch:** `make clean` drops the MySQL volume. The next
`make dev` re-applies Flyway migrations and re-runs `factory.sh` against an
empty database.

---

## 4. Common workflows

```bash
# First-time setup
cp .env.example .env && cp frontend/.env.example frontend/.env
# (edit both files)
make dev

# After pulling new backend changes
make down
make dev          # rebuilds the backend image, re-seeds

# Reset everything
make clean
make dev

# Stop the stack
make down

# Frontend dev server (HMR) — backend can be running via make dev in another terminal
cd frontend
npm install
npm run dev
```

---

## 5. Tests

```bash
make test             # backend + frontend unit tests
make test-backend     # JUnit + Mockito + H2
make test-frontend    # Vitest
make test-e2e         # Cypress (stack must already be running)
make test-coverage    # JaCoCo report — backend/target/site/jacoco/index.html
```

---

## 6. Where things live

```
.
├── Makefile              # entry point — make dev / prod / clean / logs
├── compose.yaml          # dev compose (builds backend image locally)
├── compose.prod.yaml     # prod compose (pulls images from registry)
├── .env                  # root env (mysql + jwt)
├── scripts/
│   └── factory.sh        # idempotent seed script
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/...      # Spring Boot app + Flyway migrations
└── frontend/
    ├── Dockerfile
    ├── nginx.conf        # /api proxy + SPA fallback
    ├── .env              # Vite env (VITE_API_URL, VITE_APP_TITLE)
    └── src/...           # Vue 3 app
```

API base URL: `http://localhost:8080/api/v1/`. Swagger:
`http://localhost:8080/swagger-ui.html`.
