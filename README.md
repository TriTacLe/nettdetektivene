# Nettdetektivene

IDATT2106 - System Development 2 with Agile Project  
Team 05 - Spring 2026

A gamified educational platform teaching children (grades 3-7) about digital safety, misinformation, and cybercrime through interactive detective gameplay.

---

## Project Overview

Nettdetektivene is a web application where students become digital detectives, solving tasks related to:

- Identifying fake news and misinformation
- Recognizing phishing attempts and suspicious emails
- Creating strong passwords
- Understanding safe social media practices
- Detecting manipulated images

Teachers can create classrooms, track student progress, and manage learning content.

---

## Technology Stack

### Backend

- **Framework:** Spring Boot 3.5
- **Language:** Java 21
- **Build Tool:** Maven
- **Database:** MySQL 8.0
- **Security:** JWT Authentication, Spring Security
- **Documentation:** OpenAPI/Swagger
- **Migrations:** Flyway
- **Testing:** JUnit 5, Mockito, Testcontainers

### Frontend

- **Framework:** Vue 3
- **Language:** TypeScript
- **Build Tool:** Vite
- **Styling:** Tailwind CSS v4
- **State Management:** Pinia
- **Routing:** Vue Router
- **HTTP Client:** Axios

### DevOps

- **Version Control:** GitHub
- **CI/CD:** GitHub Actions
- **Containerization:** Docker, Docker Compose

---

## Quick Start

### Prerequisites

- Java 21
- Node.js 22
- Maven (or use wrapper: `./mvnw`)
- MySQL 8.0+ (or Docker)

### 1. Clone Repository

```bash
git clone git@git.ntnu.no:smblund/idatt2106-v26-05.git
cd idatt2106-v26-05
```

### 2. Database Setup

**Option A: Docker (Recommended)**

```bash
docker-compose up -d mysql
```

**Option B: Local MySQL**

```bash
mysql -u root -p -e "CREATE DATABASE nettdetektivene;"
```

### 3. Backend Setup

```bash
cd backend
cp .env.example .env
# Edit .env with your database credentials
./mvnw spring-boot:run
```

Backend runs at: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html

### 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at: http://localhost:5173

---

## Project Structure

```
idatt2106-v26-05/
├── backend/
│   ├── src/
│   │   ├── main/java/ntnu/no/backend/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Exception handlers
│   │   │   ├── mapper/          # Entity-DTO mappers
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── security/        # JWT and security
│   │   │   ├── service/         # Business logic
│   │   │   └── shared/          # Shared utilities
│   │   ├── resources/
│   │   │   └── db/migration/    # Flyway migrations
│   │   └── test/                # Test classes
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/          # Vue components
│   │   ├── views/               # Page views
│   │   ├── services/            # API services
│   │   ├── stores/              # Pinia stores
│   │   ├── router/              # Vue Router config
│   │   └── types/               # TypeScript types
│   └── package.json
└── compose.yml
```

---

## Team

| Name                     | Role                            | Responsibility                                     |
| ------------------------ | ------------------------------- | -------------------------------------------------- |
| Nora Lilliedahl Kirknes  | Process Documentation / Backend | Auth, Security, Process                            |
| Tri Tac Le               | Backend                         | Achievement, Task, Notebook, Mystery, Notification |
| Anine Selle Standnes     | Backend                         | User, Classroom                                    |
| Silje Marie Borgan Lund  | Frontend                        | Views, Components                                  |
| Regine Hjelmtveit        | Frontend                        | UI/UX, Styling                                     |
| Vetle Nilsen             | Frontend                        | Game Logic, Interactions                           |
| Gustav Haverstad Skyberg | Frontend                        | Integration, Testing                               |

**Product Owners:** Lea Mæhlum, Malin

---

## API Documentation

All API endpoints are documented via Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Base URL: `http://localhost:8080/api/v1/`

## Development

### Running Tests

**Backend:**

```bash
cd backend
./mvnw test
./mvnw test jacoco:report  # With coverage
```

**Frontend:**

```bash
cd frontend
npm run test:unit
```

### Code Style

- Follow existing naming conventions
- Use constructor injection (not @Autowired)
- Map entities to DTOs before returning from controllers
- Add @PreAuthorize on all mutating endpoints
- Generate UUIDs in Java (not database)

---

## Documentation

- [CHANGELOG.md](CHANGELOG.md) - Sprint-by-sprint changelog

---
