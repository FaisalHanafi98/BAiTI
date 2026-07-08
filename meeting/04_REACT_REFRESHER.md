# React 18 + TypeScript + Vite — Refresher Cheat Sheet

> **Audience**: You (Faisal). You've used React in Auto-Recruit and you've written TypeScript before. This file is a high-density refresher of the modern React stack we're using on MyComunity, plus the libraries that make it actually pleasant to work with.

---

## 1. The modern React stack (what we're using and why)

| Tool | Purpose | Why this one |
|---|---|---|
| **React 18** | UI library | Industry standard. Hooks-based functional components. |
| **TypeScript** | Static typing | Catches bugs at compile time. Auto-complete everywhere. |
| **Vite** | Dev server + build tool | Replaces webpack. Starts in <1 sec. Hot reload is instant. |
| **React Router v6** | Client-side routing | The routing standard. v6 has a much cleaner API than v5. |
| **TanStack Query** (formerly React Query) | Server state management | Handles loading, caching, refetching, retries — kills 80% of useEffect spaghetti. |
| **Zustand** | Client state (light) | Tiny, fast, no boilerplate. Better than Redux for our scale. |
| **React Hook Form** | Forms | Uncontrolled inputs, fast renders, great DX. |
| **Zod** | Validation schemas | Type-safe validation. Pairs perfectly with React Hook Form. |
| **Axios** | HTTP client | Interceptors for JWT refresh, better error handling than fetch. |
| **Tailwind CSS** | Styling | Utility classes. No more naming CSS classes. Fast iteration. |
| **Headless UI** | Unstyled accessible components | Modals, dropdowns, tabs — accessible by default, style with Tailwind. |
| **Vitest** | Test runner | Vite-native. Same API as Jest, way faster. |
| **React Testing Library** | Component testing | Tests behavior, not implementation. |

---

## 2. Functional components — the only kind we write

```tsx
// ❌ Old class style — don't write this
class Counter extends React.Component { ... }

// ✅ Modern functional component
import { useState } from 'react';

type CounterProps = {
  initialValue?: number;
  onChange?: (value: number) => void;
};

export function Counter({ initialValue = 0, onChange }: CounterProps) {
  const [count, setCount] = useState(initialValue);

  const increment = () => {
    const next = count + 1;
    setCount(next);
    onChange?.(next);
  };

  return (
    <button
      onClick={increment}
      className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
    >
      Count: {count}
    </button>
  );
}
```

Notes:
- Props are typed via a `type` (or `interface`)
- `useState` returns `[currentValue, setterFunction]`
- Optional props get defaults via destructuring (`initialValue = 0`)
- `onChange?.(next)` = optional chaining (only call if `onChange` was passed)
- Tailwind classes are inline — no separate CSS file

---

## 3. The 6 hooks you'll use 95% of the time

### `useState` — local state
```tsx
const [open, setOpen] = useState(false);
```

### `useEffect` — side effects (avoid for fetching — use TanStack Query instead!)
```tsx
useEffect(() => {
  document.title = `Complaints (${count})`;
  return () => {
    // cleanup if needed
  };
}, [count]);  // dependency array — re-run when count changes
```

**Rule**: If you're fetching data with `useEffect`, you're doing it wrong in 2026. Use TanStack Query.

### `useMemo` — expensive computations
```tsx
const filteredComplaints = useMemo(
  () => complaints.filter(c => c.status === 'OPEN'),
  [complaints]
);
```

### `useCallback` — stable function references
```tsx
const handleSubmit = useCallback((data: FormData) => {
  // ...
}, [/* deps */]);
```

Use only when passing callbacks to memoized children. Don't sprinkle it everywhere.

### `useContext` — share data without prop drilling
```tsx
const theme = useContext(ThemeContext);
```

### Custom hooks — extract reusable logic
```tsx
function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
}
```

Custom hooks are just functions that start with `use`. They can call other hooks. They're how you share stateful logic between components.

---

## 4. TypeScript with React — the patterns

### Typing props
```tsx
type ButtonProps = {
  variant: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  onClick: () => void;
  children: React.ReactNode;
};

export function Button({ variant, size = 'md', disabled, onClick, children }: ButtonProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`btn btn-${variant} btn-${size}`}
    >
      {children}
    </button>
  );
}
```

### Typing event handlers
```tsx
const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  setValue(e.target.value);
};

const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
  e.preventDefault();
  // ...
};
```

### Typing API responses (auto-generated from backend!)
We'll use `openapi-typescript` to generate types from the backend's OpenAPI spec. The frontend `api/types.ts` file is **never written by hand** — it's regenerated from the backend. If the backend renames a field, the frontend stops compiling. That's the whole point.

```tsx
import type { components } from './api/types.gen';
type Complaint = components['schemas']['ComplaintResponse'];
```

---

## 5. Data fetching with TanStack Query — the killer library

### Setup (once, in App.tsx)
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 60_000 } },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router />
    </QueryClientProvider>
  );
}
```

### Fetching data
```tsx
import { useQuery } from '@tanstack/react-query';

function ComplaintList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['complaints'],
    queryFn: async () => {
      const response = await axios.get<Complaint[]>('/api/complaints');
      return response.data;
    },
  });

  if (isLoading) return <div>Loading…</div>;
  if (error) return <div>Failed to load complaints</div>;

  return (
    <ul>
      {data?.map(c => <li key={c.id}>{c.title}</li>)}
    </ul>
  );
}
```

**What this gives you for free**:
- Automatic loading state (`isLoading`)
- Automatic error state (`error`)
- Caching (don't re-fetch if you already have it)
- Automatic refetch on window focus
- Deduplication (10 components asking for the same data = 1 request)
- Background refetching to keep data fresh

**Compare to old-school useEffect**:
```tsx
// ❌ The old way — don't do this
const [data, setData] = useState(null);
const [loading, setLoading] = useState(true);
const [error, setError] = useState(null);

useEffect(() => {
  axios.get('/api/complaints')
    .then(res => setData(res.data))
    .catch(err => setError(err))
    .finally(() => setLoading(false));
}, []);
// 12 lines, no caching, no dedupe, no refetch, easy to break
```

### Mutations (POST/PUT/DELETE)
```tsx
import { useMutation, useQueryClient } from '@tanstack/react-query';

function CreateComplaintForm() {
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (data: CreateComplaintRequest) =>
      axios.post<Complaint>('/api/complaints', data),
    onSuccess: () => {
      // Refetch the list after creating
      queryClient.invalidateQueries({ queryKey: ['complaints'] });
    },
  });

  return (
    <button
      onClick={() => createMutation.mutate({ title: '...', description: '...', categoryId: '...' })}
      disabled={createMutation.isPending}
    >
      {createMutation.isPending ? 'Creating…' : 'Create'}
    </button>
  );
}
```

---

## 6. Routing with React Router v6

```tsx
import { createBrowserRouter, RouterProvider, Outlet } from 'react-router-dom';

const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,                    // Shared layout
    children: [
      { index: true, element: <Dashboard /> },  // /
      { path: 'complaints', element: <ComplaintList /> },
      { path: 'complaints/:id', element: <ComplaintDetail /> },
      { path: 'login', element: <Login /> },
    ],
  },
]);

function App() {
  return <RouterProvider router={router} />;
}
```

### Reading URL params
```tsx
import { useParams } from 'react-router-dom';

function ComplaintDetail() {
  const { id } = useParams<{ id: string }>();
  // use id to fetch the specific complaint
}
```

### Navigation
```tsx
import { useNavigate, Link } from 'react-router-dom';

function MyComponent() {
  const navigate = useNavigate();
  return (
    <>
      <Link to="/complaints">All Complaints</Link>
      <button onClick={() => navigate('/complaints/123')}>Go</button>
    </>
  );
}
```

### Protected routes
```tsx
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user } = useAuthStore();
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
```

---

## 7. Forms with React Hook Form + Zod

```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const schema = z.object({
  title: z.string().min(1, 'Title is required').max(200),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  categoryId: z.string().uuid(),
});

type FormData = z.infer<typeof schema>;   // Auto-derived TypeScript type

function CreateComplaintForm() {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = (data: FormData) => {
    // data is fully typed and validated
    console.log(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('title')} placeholder="Title" />
      {errors.title && <span>{errors.title.message}</span>}

      <textarea {...register('description')} placeholder="Description" />
      {errors.description && <span>{errors.description.message}</span>}

      <button type="submit">Submit</button>
    </form>
  );
}
```

Why this combo is amazing:
- One source of truth: Zod schema → both validation AND TypeScript type
- Uncontrolled inputs = fast (no re-render on every keystroke)
- Errors are typed
- Form state managed for you

---

## 8. Global state with Zustand (when you actually need it)

Use Zustand for **client state that multiple components need** (auth user, theme, sidebar open/closed). Use TanStack Query for **server state**. Don't mix the two.

```tsx
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type AuthState = {
  user: User | null;
  token: string | null;
  login: (user: User, token: string) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      login: (user, token) => set({ user, token }),
      logout: () => set({ user: null, token: null }),
    }),
    { name: 'auth-storage' }
  )
);

// Usage in any component:
function Header() {
  const user = useAuthStore(s => s.user);
  const logout = useAuthStore(s => s.logout);
  return <div>{user?.name} <button onClick={logout}>Logout</button></div>;
}
```

That's the entire global state setup. No actions, no reducers, no Provider. Compare to Redux: ~10x less code.

---

## 9. Axios with JWT interceptor

```tsx
import axios from 'axios';
import { useAuthStore } from './stores/auth';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,    // From .env
});

// Attach JWT to every request
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  // For multi-tenant in dev: attach tenant slug header
  config.headers['X-Tenant-Slug'] = import.meta.env.VITE_TENANT_SLUG;
  return config;
});

// Auto-logout on 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 10. Tailwind essentials

```tsx
<div className="flex items-center justify-between p-4 bg-white shadow rounded-lg">
  <h2 className="text-xl font-semibold text-gray-900">Complaints</h2>
  <button className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition">
    New
  </button>
</div>
```

The mental model:
- `flex items-center justify-between` = flexbox row, vertically centered, space between
- `p-4` = padding 1rem (16px)
- `bg-white` = background white
- `shadow rounded-lg` = drop shadow + 8px border radius
- `text-xl font-semibold` = font size 1.25rem, bold-ish
- `hover:bg-blue-700` = on hover, darker blue

Sizes are on a scale: `0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32...` where 4 = 1rem.

**You will Google Tailwind classes constantly. That's normal.** The Tailwind docs and the VS Code IntelliSense extension make it fast.

---

## 11. Vite project structure

```
frontend/
├── index.html                    # Entry HTML
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
├── src/
│   ├── main.tsx                  # Entry point — renders <App />
│   ├── App.tsx                   # Root component, Router + QueryClient setup
│   ├── api/
│   │   ├── client.ts             # Axios instance
│   │   └── types.gen.ts          # AUTO-GENERATED from backend OpenAPI
│   ├── stores/
│   │   └── auth.ts               # Zustand auth store
│   ├── pages/
│   │   ├── Login.tsx
│   │   ├── Dashboard.tsx
│   │   └── complaints/
│   │       ├── ComplaintList.tsx
│   │       └── ComplaintDetail.tsx
│   ├── components/
│   │   ├── ui/                   # Generic Button, Input, Modal, etc.
│   │   └── complaints/           # ComplaintCard, ComplaintForm, etc.
│   ├── hooks/
│   │   └── useDebounce.ts
│   └── lib/
│       └── utils.ts
└── tests/                        # Vitest tests
```

---

## 12. The 5 commands you'll run

```bash
npm install              # Install deps
npm run dev              # Start Vite dev server (default port 5173)
npm run build            # Production build → dist/
npm run test             # Run Vitest
npm run lint             # ESLint check
```

---

## 13. Common pitfalls

| Pitfall | Fix |
|---|---|
| Fetching data in `useEffect` | Use TanStack Query instead. Always. |
| Mutating state directly (`array.push(...)`) | Use the setter with a new array: `setItems([...items, newItem])` |
| Forgetting `key` prop in lists | Always provide a stable, unique `key` (use `id`, not array index) |
| Putting all state in Redux/Zustand | Server state → TanStack Query. Form state → React Hook Form. Only truly global UI state → Zustand. |
| Re-rendering everything on every keystroke | Use uncontrolled inputs via React Hook Form |
| `useEffect` infinite loops | Check the dependency array. Probably you're setting state that's also a dep. |
| Class components | Don't write them. Functional only. |
| `any` everywhere in TypeScript | Take the time to type properly. `unknown` is better than `any`. |

---

## 14. The mental model in one paragraph

A React app is a tree of functional components. Each component is a function that takes props and returns JSX. State lives in components via `useState` (local) or Zustand (global UI). Server data lives in TanStack Query, which handles fetching, caching, and refetching automatically. Routes are defined in React Router. Forms use React Hook Form with Zod schemas for type-safe validation. Everything is styled with Tailwind utility classes. Vite is the dev server and bundler. TypeScript catches type errors at compile time, and the API types come from the backend's OpenAPI spec so frontend and backend can never drift.

---

## 15. If Anep asks you to explain modern React in 60 seconds

> "Modern React is functional components and hooks — no classes. State is managed with `useState` for local stuff. We use TanStack Query for server data, which gives us caching, loading, and error states for free. Forms are React Hook Form plus Zod for validation. Routing is React Router v6. Styling is Tailwind utility classes. TypeScript everywhere, and the API types come straight from the backend's OpenAPI spec so they can't drift. Vite is the build tool — way faster than webpack. Tests run on Vitest. The whole stack is the current industry standard, and it ports cleanly to React Native when we go mobile."

That's it. He'll get it.
