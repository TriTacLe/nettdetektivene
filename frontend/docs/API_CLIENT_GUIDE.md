# OpenAPI Client Generation Guide

How to auto-generate TypeScript API clients from your Spring Boot backend.

---

## Technology Overview

| Technology | What it is |
|------------|-----------|
| **OpenAPI** | A standard format (JSON/YAML) for describing REST APIs — endpoints, request bodies, response shapes, and auth. Your backend produces this automatically via springdoc-openapi. |
| **springdoc-openapi** | A Spring Boot library that reads your `@RestController` annotations and generates an OpenAPI spec at runtime. No manual spec writing needed. |
| **@hey-api/openapi-ts** | A CLI code generator. It reads the OpenAPI spec and outputs TypeScript files — types, API functions, and query composables. Run it once and it keeps your frontend in sync with the backend. |
| **@hey-api/client-fetch** | The runtime HTTP client used by the generated code. Wraps the browser `fetch` API with type-safe request/response handling and an interceptor system for auth headers. |
| **TanStack Query (Vue Query)** | A data-fetching library. It wraps API calls with caching, loading/error state, background refetching, and automatic deduplication. You describe *what* data you want, it handles *when* and *how* to fetch it. |
| **Composable** | A Vue 3 function (prefixed `use`) that encapsulates reactive state and logic. The generated `vue-query.gen.ts` exports composables like `useGetAllTasksQuery()` that are ready to call directly in `<script setup>`. |
| **Interceptor** | A function that runs on every request or response before it reaches your code. Used here to attach the `Authorization: Bearer <token>` header automatically, so individual API calls don't need to handle auth manually. |

---

## Why Switch to a Generated Client

**The problem with manual services:**

The current codebase has `types/index.ts` (hand-written DTOs) and individual service files (`AuthService.ts`, `ClassroomService.ts`, etc.) that call the backend manually. This creates several problems:

- **Drift**: when the backend changes a field name or adds a required parameter, the frontend doesn't know until something breaks at runtime.
- **Maintenance**: every new endpoint requires writing a function, a type, and wiring it up manually.
- **No loading/error state**: each component that calls a service has to manage `isLoading`, `error`, and `data` itself.

**What the generated client gives you:**

| Before | After |
|--------|-------|
| Manually written `interface TaskDTO` | Generated from backend `@Schema` — always correct |
| `apiClient.get('/tasks')` returns `any` | `getAllTasks()` returns `Promise<TaskDTO[]>` with full type safety |
| Each component manages its own loading state | `const { data, isLoading, error } = useGetAllTasksQuery()` — built in |
| Backend renames a field — silent bug | TypeScript compile error immediately |
| New endpoint — write function, type, service file | Run `npm run generate-api` — done |
| Two components fetch the same data — two requests | TanStack Query deduplicates to one request automatically |

**The workflow becomes:**

```
Backend changes endpoint  →  npm run generate-api  →  TypeScript shows what broke  →  fix and ship
```

Instead of:

```
Backend changes endpoint  →  nothing breaks at compile time  →  bug reaches testing or production
```

---

## Overview

Your backend has **springdoc-openapi** which exposes:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

We use this JSON spec to generate a fully-typed TypeScript client.

> **Version note:** This guide targets `@hey-api/openapi-ts` v0.96+ and `@hey-api/client-fetch` v0.13+.
> The config format changed significantly from older versions — `client` and `services` are no longer top-level fields.

---

## What Gets Generated

```
src/generated-client/
├── types.gen.ts      # All DTOs (TaskDTO, ClassroomDTO, etc.)
├── sdk.gen.ts        # API functions (getTasks(), createClassroom())
├── @tanstack/
│   └── vue-query.gen.ts  # Ready-to-use useQuery/useMutation composables
└── index.ts          # Barrel exports
```

---

## Step 1: Install Dependencies

```bash
npm install @hey-api/client-fetch @tanstack/vue-query
npm install -D @hey-api/openapi-ts
```

| Package | Purpose |
|---------|---------|
| `@hey-api/openapi-ts` | CLI tool that generates TypeScript from OpenAPI spec |
| `@hey-api/client-fetch` | Runtime HTTP client (fetch-based) |
| `@tanstack/vue-query` | Data fetching with caching |

---

## Step 2: Create Configuration File

Create `openapi-ts.config.ts` in the frontend root:

```typescript
import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'http://localhost:8080/v3/api-docs',
  output: {
    path: 'src/generated-client',
  },
  plugins: [
    '@hey-api/typescript',       // generates types.gen.ts
    '@hey-api/client-fetch',     // generates client setup
    '@hey-api/sdk',              // generates sdk.gen.ts (API functions)
    '@tanstack/vue-query',       // generates vue-query.gen.ts (composables)
  ],
});
```

> **Why `plugins` and not `client`/`services`?**
> In v0.96+, everything is plugin-based. The old `client` and `services` top-level fields no longer exist.

---

## Step 3: Add NPM Script

Add to `package.json`:

```json
{
  "scripts": {
    "generate-api": "openapi-ts"
  }
}
```

**Usage:**
```bash
npm run generate-api
```

Run this whenever the backend API changes.

---

## Step 4: Configure the HTTP Client with Auth

Create `src/lib/GeneratedClient.ts`:

```typescript
import { client } from '@/generated-client/client.gen'

const SESSION_KEY = 'auth'

function getToken(): string | undefined {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as { token?: string }).token : undefined
  } catch {
    return undefined
  }
}

client.setConfig({ baseUrl: 'http://localhost:8080' })

client.interceptors.request.use((request) => {
  const token = getToken()
  if (token) request.headers.set('Authorization', `Bearer ${token}`)
  return request
})
```

> **Important:** Import `client` from `client.gen.ts` (the generated file), not from
> `@hey-api/client-fetch`. The SDK functions use that specific instance — configuring
> a separate client created with `createClient()` has no effect on the generated SDK.
>
> The base URL is `http://localhost:8080` (no `/api/v1`). The generated SDK functions
> already include `/api/v1` in their paths — adding it to the base URL doubles it.
>
> The session key `'auth'` matches the Pinia auth store's `persist.storage` key.

---

## Step 5: Setup TanStack Query (Vue Query)

Update `src/main.ts` to import the client setup and register TanStack Query:

```typescript
import { createApp } from 'vue';
import { VueQueryPlugin } from '@tanstack/vue-query';
import '@/lib/GeneratedClient';  // registers base URL + auth interceptor
import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(router);
app.use(VueQueryPlugin);

app.mount('#app');
```

> The `import '@/lib/GeneratedClient'` runs the module as a side effect —
> it configures the generated client instance before any components mount.

---

## How It Works

### Before (Manual)

```typescript
// types/index.ts - manually written, can drift from backend
export interface TaskDTO {
  id: string;
  title: string;
  taskType: TaskType;
}

// services/TaskService.ts - manually maintained
async function getTasks(): Promise<TaskDTO[]> {
  const { data } = await apiClient.get('/tasks');
  return data;
}
```

### After (Generated)

```typescript
// Generated automatically from OpenAPI spec — always in sync
import { useGetAllTasksQuery } from '@/generated-client';

// Types and API calls are derived directly from the backend spec
```

---

## Usage Examples

### Example 1: Query with generated composable (preferred)

The `@tanstack/vue-query` plugin generates composables directly. No manual `useQuery` wrapping needed:

```vue
<script setup lang="ts">
import { useGetAllTasksQuery } from '@/generated-client/@tanstack/vue-query.gen';

const { data: tasks, isLoading, error } = useGetAllTasksQuery();
</script>

<template>
  <div v-if="isLoading">Loading...</div>
  <div v-else-if="error">Error: {{ error.message }}</div>
  <ul v-else>
    <li v-for="task in tasks?.data" :key="task.id">{{ task.title }}</li>
  </ul>
</template>
```

### Example 2: Mutation with invalidation

```vue
<script setup lang="ts">
import { useQueryClient, useMutation } from '@tanstack/vue-query';
import { createTask } from '@/generated-client/sdk.gen';
import type { TaskRequestDTO } from '@/generated-client/types.gen';

const queryClient = useQueryClient();

const mutation = useMutation({
  mutationFn: (newTask: TaskRequestDTO) => createTask({ body: newTask }),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['tasks'] });
  },
});

function handleSubmit() {
  mutation.mutate({
    title: 'New Task',
    taskType: 'NEWS',
    difficulty: 1,
    content: '{}',
  });
}
</script>
```

### Example 3: Dynamic query with path parameter

```typescript
import { useQuery } from '@tanstack/vue-query';
import { getTaskById } from '@/generated-client/sdk.gen';
import { computed } from 'vue';

const props = defineProps<{ taskId: string }>();

const { data: task } = useQuery({
  queryKey: computed(() => ['task', props.taskId]),
  queryFn: () => getTaskById({ path: { id: props.taskId } }),
  enabled: computed(() => !!props.taskId),
});
```

---

## TanStack Query Benefits

| Feature | What It Does |
|---------|--------------|
| **Caching** | Stores data in memory, avoids redundant API calls |
| **Background Refetch** | Updates stale data automatically when window regains focus |
| **Loading States** | `isLoading`, `isFetching` booleans built-in |
| **Error Handling** | `error` object with automatic retry |
| **Deduplication** | Same query key = single request shared across components |

---

## Workflow

```
1. Backend developer adds/changes endpoint
        |
        v
2. Run: npm run generate-api
        |
        v
3. TypeScript types and composables update automatically
        |
        v
4. Frontend code shows type errors if incompatible
        |
        v
5. Fix frontend, rebuild, deploy
```

---

## Best Practices

### Always generate after backend changes

```bash
cd frontend
npm run generate-api
```

### Commit generated code

```bash
git add src/generated-client/
git commit -m "Update generated API client"
```

This means teammates don't need the backend running to get type-correct imports.

### Use query keys consistently

```typescript
queryClient.invalidateQueries({ queryKey: ['classroom', classroomId] });
```

### Don't mix manual and generated types

```typescript
// Bad — keeping old manual types
import type { TaskDTO } from '@/types';

// Good — use generated
import type { TaskDTO } from '@/generated-client/types.gen';
```

---

## Troubleshooting

### Backend not running during generation

```bash
# Option 1: Start backend first
cd ../backend && ./mvnw spring-boot:run

# Option 2: Save spec to file and point config at it
curl http://localhost:8080/api-docs > api-spec.json
# Then in openapi-ts.config.ts: input: './api-spec.json'
```

### Type errors after generation

```bash
rm -rf src/generated-client
npm run generate-api
```

### CORS issues during generation

The generator fetches the spec at build time from your machine, not from the browser — CORS does not apply. If generation fails with a network error, the backend is not running.

---

## Migration from Manual Services

| Old | New |
|-----|-----|
| `AuthService.login(credentials)` | `login({ body: credentials })` |
| `apiClient.get('/tasks')` | `getAllTasks()` from `sdk.gen.ts` |
| Manual `types/index.ts` | `types.gen.ts` (auto-generated) |
| Manual `useQuery` wrapper | Generated composable from `vue-query.gen.ts` |
| `apiClient` interceptor | `generatedClient.interceptors` in `GeneratedClient.ts` |

---

## Summary

1. **Config**: `openapi-ts.config.ts` with `plugins` array (not `client`/`services`)
2. **Auth**: configure via interceptor in `src/lib/GeneratedClient.ts`
3. **Generate**: `npm run generate-api` creates types, SDK functions, and Vue Query composables
4. **Query**: use generated composables from `vue-query.gen.ts` directly
5. **Mutate**: use `useMutation()` wrapping generated SDK functions
6. **Invalidate**: refresh related queries after mutations
