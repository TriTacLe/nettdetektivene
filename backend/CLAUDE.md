# Backend Implementasjonsplan

## 1. Layers og avhengigheter

| Layer | Avhenger av | Tabeller / entiteter |
|-------|-------------|---------------------|
| 0 | - | role, app_user, institution, achievement |
| 1 | Layer 0 | app_user_local_credential, app_user_refresh_token, app_user_identity, user_role, user_profile, classroom |
| 2 | Layer 1 | task, classroom_instructor, classroom_student, classroom_task |
| 3 | Layer 2 | task_best_score, user_achievement, notebook_entry, mystery_submission, notification |

Viktig å ikke havne i situasjon der man jobbe med X avhenger av Y og Y ikke er implementert enda

## 2. Ansvarsfordeling

| Person | Ansvarsområde |
|--------|---------------|
| A - Nora, Anine | Auth, user_profile, classroom, institution, classroom_instructor, classroom_student |
| B - Tri | Achivement, task, classroom_task, task_attempt, task_best_score, user_achivement, notebook_entry, mystery_submission, notification |

Kode mot entity-og repository-interfacet, ikke mot services - da blokkerer ikke A og B hverandre.

## 3. Filstruktur og navnkonvensjon på filer

Spring Boot layered architecture:


**Navnkonvensjon:**

| Type | Konvensjon | Eksempel |
|------|------------|----------|
| DTO | XxxRequestDTO, XxxReponseDTO | LoginRequestDTO |
| Controller | XxxController | AuthController |
| Service | XxxService | UserService |
| Entity | db tabell er app_user, entity blir CamelCase | AppUser |

## 4. Steg for koding

### Steg 0 - Felles FØR koding av faktisk kode

Avtal application.properties, pom, .env, og alt av configs i lag og publiser dette til github før neste steg

Deretter avtal og lag klassene som brukes i både A og B sine implementasjoner: ApiResonse<T> og GlobalExceptionHandler - skriv disse før noen lager eneste controller. Legger ApiResponse i shared/ og GlobalExepcetionHandler i exception/

- **ApiResponse<T>** er en wrapper klasse som sikrer at alle endepunkter returnerer samme JSON-struktur. Frontend vet hva man alltid får. Brukes i alle kontrollere
- **GlobalExeceptionHandler** fanger alle ubehandlede exceptions automatisk og returnerer dem som ApiResponse istedenfor Spring sin rå strack trace. Lager exeception for hvert error kode 404, 403, 401, 400, 500

Publiser alt dette til github før neste steg

### Steg 1 - Entities og repositories

Starter her med entities og repo da vi ikke har service-logikk og ingen risiko for sirkulære avhengigheter. Publiser til github når Layer 0 er ferdig da resten av teamet avhenger av disse.

### Steg 2 - Auth (JWT og security config)

Avhenger ikke av noe og kan startes med en gang. Må være ferdig før noen endepunkter kan fungere.

- **JwtUtil** - genererer og validerer access tokens
- **JwtAuthFilter** - OncePerRequestFilter, henter bruker fra header
- **SecurityConfig** - tillater /api/v1/auth/**, sperrer alt annet, konfigurerer CORS og CSRF osv

### Steg 3 - DTO og mappers

DTO og mapper blir ikke blokkert av noen

### Steg 4 - Service layer

**kun repo-avhengigheter:**
- UserService
- RoleService
- InstitutionService
- AchievementService

**service-avhengigheter:**
1. UserProfileService - avhenger av UserService
2. ClassroomService - avhenger av UserService, InstitutionService, UserProfileService
3. ClassroomMemberService - avhenger av ClassroomService, UserService
4. TaskService - avhenger av UserService
5. TaskAttemptService - avhenger av TaskService, ClassroomService
6. NotebookService - avhenger av UserService, ClassroomService
7. MysteryService - avhenger av ClassroomService, UserService, AchievementService
8. NotificationService - avhenger av UserService, ClassroomService

**OBS - Triggers og stored procedures:**

Triggers er MySQL-kode som kjører automatisk. Ikke reimplementer denne logikken i Java.

| Navn | Timing | Hva den gjør |
|------|--------|--------------|
| trg_classroom_auto_instructor | AFTER INSERT classroom | Auto-inserter oppretter som instructor - ikke gjør dette manuelt i ClassroomService |
| trg_attempt_sync | AFTER INSERT task_attempt | Upsert av task_best_score, tildeler 10 XP kun ved første bestått, oppdaterer xp_level |
| trg_mystery_approved | AFTER UPDATE mystery_submission | Oppretter notifikasjon og tildeler achievement ved godkjenning - idempotent via INSERT IGNORE |
| sp_submit_attempt | Stored procedure | Eneste inngangspunkt for å registrere forsøk. Kall via JdbcTemplate. Les tilbake state etterpå - ikke beregn det selv |

### Steg 5 - Endepunkter

Basestruktur: localhost:8080/api/v1/...

TODO: hvilke endepunkter trengs @Frontend?

### Steg 6 - Tester (viktig!)

- **Unit:** Service-logikk isolert fra DB med JUnit 5 + Mockito
- **Integration:** Testcontainers med MySQL image. Nødvendig for stored procedures og triggers som ikke kan mockes med H2.
- **Controller:** @WebMvcTest + MockMvc som verifiserer HTTP-statuskoder og JSON-struktur
- Integrasjonstester ligger adskilt fra unit-tester under test/.../integration/ da de er tregere og bør kjøres separat
- Bruker ikke H2!
- Tommelfingerregel: test logikk du selv har skrevet. Alt autogenerert av Spring testes indirekte via integrasjonstester
- Før push skal alle ting som er lagd testes og at testene er grønne. Trengs ikke å teste alt – bare det som er relevant. F.eks. entiteter og DTO trengs ikke å testes.

## 5. Samarbeid A og B

- GJORT! Dag 1 - sett opp configs i lag: application.properties, ApiResponse<T>, GlobalExceptionHandler
- A eier security/ - B rører aldri disse filene
- Delte filer (ApiResponse, GlobalExceptionHandler, pom.xml) - avtal endringer muntlig først
- Code review: A reviewer B sine PRer og omvendt

## 6. Miljøvariabel og andre configs

Bruker application.properties. Hemmeligheter hentes fra .env via spring-dotenv dependency.

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

.env ligger i rotmappen og committes aldri. Commit .env.example som mal for teamet.

```env
DB_URL=jdbc:mysql://localhost:3306/
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=kjør openssl rand –base64 32
JWT_EXPIRATION_MS=86400000
```

## 7. Databasetabeller

task_best_score er oppdatert av trigger trg_attempt_sync. Aldri write til denne tabellen, altså read-only og ingen save() calls

Når vi lager entitetsklasser speiler vi db tabellene

## 999. Huskeregler


- Aldri returner JPA-entiteter fra kontrollere - mapper alltid til DTO
- Rollesjekk på hvert endepunkt med @PreAuthorize for alt som modifiserer classroom-data
- Alle repositories skrives som interface og extender JpaRepository - Spring implementerer dem automatisk
- Bruk springdoc-openapi fra dag én - frontend tester på /swagger-ui.html uten å vente på backend endringer
- Felles ApiResponse<T> i shared/ - ResponseEntity styrer HTTP-laget, ApiResponse<T> styrer JSON-innholdet
- Global exception handler i exception/ - uten denne leaker Spring stack traces til frontend
- Input validation med @Valid og Bean Validation - ellers kan alle sende tomme strenger og null-felter
- Soft delete: alle repository-queries mot classroom må filtrere på deleted At IS NULL
- ENUM-kolonner: alltid @Enumerated(EnumType.STRING) - verdiene må matche MySQL ENUM eksakt
- UUID genereres i Java med UUID.randomUUID().toString() - ikke la databasen generere disse
- Seed-data: rollene STUDENT og TEACHER må ligge i Flyway-migrasjon fra dag én
- Brancher fra issues, f.eks. feature/7-auth-jwt
- Baseendepunkt alltid: localhost:8080/api/v1/
- Ingen start imports som import lombok.*

### `GlobalExceptionHandler` — `exception/`
Catches all unhandled exceptions and returns them as `ApiResponse` instead of Spring's raw stack trace. Define separate exception classes for 400, 401, 403, 404, 500.


## Pull Request and commit messages guidelines
- No credits to Claude
- Give simple template
- Don't assume, if you have questions ask
- No emojis silly. 
- No 
overcomplicated explainations

