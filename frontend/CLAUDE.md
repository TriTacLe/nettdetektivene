# Frontend Implementation Plan  
**Nettdetektivene – IDATT2106**  
*Vue 3 · Vite · Pinia · Tailwind CSS*

## 1. Layers and Dependencies
The frontend follows the same layered architecture as the backend.  
Always implement in order — **never start layer N before layer N-1 is complete and pushed to GitHub.**

| Layer | Depends on | Pages / Components |
|------|------------|------------------|
| 0 | – | Project setup: router, Pinia, axios client, GlobalLayout, shared types, ApiService |
| 1 | Layer 0 | Auth: LoginView, RegisterView, navigation guards, useAuthStore |
| 2 | Layer 1 | TeacherDashboard, ClassroomView, StudentHomescreen, AvatarEditor, JoinClassroom, IntroSequence |
| 3 | Layer 2 | MapView, all TaskView stops, NotebookView, BadgesView, LeaderboardView, MysteryView, NotificationView |

## 2. File Structure
```
src/
  assets/
  components/
  views/
    stops/
  stores/
  services/
  router/
  composables/
  layouts/
  types/
  utils/
```

## 3. Development Steps
### Step 0 — Setup
- Configure project files
- Setup ApiService
- Setup router and Pinia
- Setup common components (e.g. common scene components)

### Step 1 — Auth
- Start view
- Login views
- Auth store and guards

### Step 2 — Parallel Work
- A: Teacher views
- B: Student views
- C: Map + Task
- D: Notebook + Mystery + Notifications

### Step 3 — Features
- Game stops
- Notebook, Mystery, Notifications
- Audio

### Step 4 — Workflow Pipeline
- Unit: Vitest
- Component: Vue Test Utils
- E2E: Cypress/Playwright

## 6. Code Workflow Pipeline
### Step 1 - Planning
- When given a coding task you must make a plan
- Never make assumtions, ask the user if something is unclear

### Step 2 - Implementation
- After finishing planning you should start coding

### Step 3 - Review
- After you're done with coding you should review the code
- Look for vulnerabilities
- Look for inconsistenct code (e.g. variables with different names across different scopes)

## 5. Environment Variables
```
VITE_API_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=Nettdetektivene
```

## 6. Guidelines
- No axios in components
- Use Pinia + sessionStorage
- Protect routes
- No raw errors to users
- Responsive design
- WCAG compliance
- No frontend game logic
- Write tests early

## 7. Style Standard
| Category | Convention | Example |
|----------|-----------|---------|
| Files (components, composables, stores, services, utils) | PascalCase | `MapView.vue`, `UseAuth.ts`, `ApiService.ts` |
| View files (pages) | PascalCase + `View` suffix | `LoginView.vue`, `TeacherDashboardView.vue` |
| Variables, functions, methods | camelCase | `fetchClassroom()`, `isLoading`, `handleSubmit` |
| Types, interfaces, enums | PascalCase | `UserProfile`, `TaskStatus`, `ClassroomResponse` |
| CSS classes | kebab-case (Tailwind utilities) | `task-card`, `nav-wrapper` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRIES`, `API_TIMEOUT` |
| Props / emits | camelCase | `studentId`, `onTaskComplete` |
`
## 8. Pull Request Guidelines
- No credits to Claude
- Give simple template
- Don't assume, if you have questions ask
- No emojis silly. 


## 9. Views and Routing
- Whenever you add a new view under `src/views/`, also register a corresponding route in `src/router/index.ts`.
- Never create a view without wiring it into the router — orphan views are not allowed.
