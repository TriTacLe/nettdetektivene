# How to Use the Generated API Client

Quick reference for frontend developers. The client is already wired up — you
just need to import and use the generated functions.

---

## The Big Picture

```
backend @RestController
        |
        v
  npm run generate-api          <- run this when the backend changes
        |
        v
  src/generated-client/
    types.gen.ts                <- all DTOs (TypeScript interfaces)
    sdk.gen.ts                  <- API functions (getAllClassrooms, etc.)
    @tanstack/vue-query.gen.ts  <- ready-made useQuery/useMutation composables
```

The auth header is attached automatically on every request. You never write
`Authorization: Bearer ...` yourself.

---

## Step 0: Regenerate after backend changes

Every time a teammate changes a backend endpoint, run this:

```bash
cd frontend
npm run generate-api
```

Then commit the updated `src/generated-client/` folder so everyone stays in sync.

> **Requirement:** The backend must be running on `localhost:8080` when you run
> this command — the generator fetches the OpenAPI spec from there.

---

## Step 1: Find the function you need

Open `src/generated-client/sdk.gen.ts` and search for your endpoint.

For example, `GET /classrooms` is `getAllClassrooms`. For path parameters like
`DELETE /classrooms/{id}` look for `deleteClassroom`.

The `@tanstack/vue-query.gen.ts` file has the same names suffixed with
`Options`, `QueryKey`, or `Mutation`:

```
getAllClassrooms        -> getAllClassroomsOptions()   (for useQuery)
                       -> getAllClassroomsQueryKey()   (for invalidation)
deleteClassroom        -> deleteClassroomMutation()   (for useMutation)
```

---

## Step 2: Fetch data with useQuery

Replace a manual `onMounted` + `ref` fetch with `useQuery`:

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { getAllClassroomsOptions } from '@/generated-client/@tanstack/vue-query.gen'

const { data, isLoading, error } = useQuery(getAllClassroomsOptions())
</script>

<template>
  <div v-if="isLoading">Laster...</div>
  <div v-else-if="error">Feil: {{ error }}</div>
  <ul v-else>
    <li v-for="c in data?.data" :key="c.id">{{ c.title }}</li>
  </ul>
</template>
```

`data.value.data` holds the response payload (the outer `data` is TanStack's
query result wrapper, the inner `.data` is the HTTP response body).

---

## Step 3: Mutate data with useMutation

For create, update, or delete operations:

```vue
<script setup lang="ts">
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import {
  deleteClassroomMutation,
  getAllClassroomsQueryKey,
} from '@/generated-client/@tanstack/vue-query.gen'

const queryClient = useQueryClient()

const deleteMutation = useMutation({
  ...deleteClassroomMutation(),
  onSuccess: () => {
    // Refetch the classroom list after a successful delete
    queryClient.invalidateQueries({ queryKey: getAllClassroomsQueryKey() })
  },
})

function handleDelete(id: string) {
  deleteMutation.mutate({ path: { id } })
}
</script>
```

The spread `...deleteClassroomMutation()` fills in `mutationFn` for you. You
only add `onSuccess`/`onError` callbacks on top.

---

## Step 4: Pass request bodies and path params

The generated functions accept a single options object. The key fields are:

| Field | When to use |
|-------|------------|
| `body` | POST/PUT/PATCH request body |
| `path` | URL path parameters (`/classrooms/{id}`) |
| `query` | URL query parameters (`?page=1`) |

```typescript
// POST with body
createClassroomMutation().mutationFn({ body: { title: 'Klasse A', joinCode: 'ABC123' } })

// DELETE with path param
deleteClassroomMutation().mutationFn({ path: { id: classroomId } })

// GET with query param
getAllTasksOptions({ query: { type: 'NEWS' } })
```

---

## Step 5: Use types from the generated client

Stop importing from `@/types` for things that exist in the generated client:

```typescript
// Before
import type { ClassroomResponseDTO } from '@/types'

// After
import type { ClassroomResponseDTO } from '@/generated-client/types.gen'
```

The generated types always match the backend exactly. The hand-written ones in
`@/types` can drift.

---

## Real Example: Migrating an existing view

Before (manual axios approach):

```typescript
import { ref, onMounted, computed } from 'vue'
import ClassroomService from '@/services/ClassroomService.ts'
import type { ClassroomResponseDTO } from '@/types'

const classrooms = ref<ClassroomResponseDTO[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    classrooms.value = await ClassroomService.getAllClassrooms()
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke hente klasserom'
  } finally {
    loading.value = false
  }
})
```

After (generated client + TanStack Query):

```typescript
import { useQuery } from '@tanstack/vue-query'
import { getAllClassroomsOptions } from '@/generated-client/@tanstack/vue-query.gen'

const { data, isLoading, error } = useQuery(getAllClassroomsOptions())
// data.value.data -> ClassroomResponseDTO[]
// isLoading.value -> boolean
// error.value     -> Error | null
```

The old service file can stay — you're not deleting anything, just not calling
it from this view. See `ClassroomOverviewView.vue` for a side-by-side comparison
with the old code preserved in comments.

---

## Troubleshooting

**403 on requests**

Check that you're logged in. The interceptor reads your token from
`sessionStorage`. If `sessionStorage.getItem('auth')` is `null` in the browser
console, log in again.

**404 / wrong URL**

The generated client uses `http://localhost:8080` as the base URL. Paths in
`sdk.gen.ts` already include `/api/v1`, so the full URL becomes
`http://localhost:8080/api/v1/classrooms`. Do not set `VITE_API_URL` as the
base URL — that would duplicate the path.

**TypeScript errors after regenerating**

A backend field was renamed or removed. TypeScript will point you to exactly
which line broke. Fix the usage to match the new type.

**`data.value` is undefined**

TanStack Query is async — `data.value` is undefined until the first successful
fetch. Always guard with `v-if="isLoading"` / `v-else` or optional chaining
`data?.data`.

**Generated files show as modified in git**

This is expected. After running `npm run generate-api`, commit the updated
`src/generated-client/` files alongside your other changes.
