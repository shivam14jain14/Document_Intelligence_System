# Frontend Coding Rules

## Tech Stack
- React 18 + TypeScript (strict mode)
- Vite for bundling
- TailwindCSS for all styling
- shadcn/ui for UI primitives
- React Query (TanStack Query) for server state
- Zustand for client state (auth token only)
- Axios for HTTP via a shared instance
- EventSource (native browser API) for SSE streaming

## File & Component Conventions
- One component per file; filename matches component name (PascalCase)
- Barrel exports (`index.ts`) allowed only at the `components/` top level
- Pages go in `src/pages/`; reusable UI in `src/components/`; data logic in `src/hooks/`
- Types go in `src/types/` — never inline complex types in components

## Component Rules
- Functional components only — no class components
- Props interface defined at top of file as `interface Props { ... }`
- Keep components focused: if a component exceeds ~150 lines, split it
- Pages own layout and orchestration; leaf components handle only display
- No data-fetching logic inside component bodies — delegate to hooks

## State Management
- Server state: React Query only (`useQuery`, `useMutation`)
- Client state: Zustand (`authStore.ts`) for JWT token + user info
- No `useState` for data that comes from the server — that belongs in React Query cache
- JWT stored in Zustand + localStorage; never in URL params or session cookies

## API Layer
- All HTTP calls go through `src/services/api.ts` (Axios instance with base URL + JWT interceptor)
- Service files per domain: `authService.ts`, `documentService.ts`, `chatService.ts`
- No raw `fetch()` or inline `axios.get()` calls inside components or hooks
- SSE streaming: use `useStreamingChat` hook backed by native `EventSource`
- 401 responses → Axios interceptor clears auth store and redirects to `/login`

## Styling
- Tailwind utility classes only — no inline styles, no `.css` files except `globals.css`
- Dark theme as default; color tokens: `bg-slate-900`, `bg-slate-800`, `border-slate-700`
- shadcn/ui components for all primitives: Button, Badge, Dialog, Table, Select, Tooltip, Skeleton
- Never recreate components that exist in shadcn/ui (no custom Button, no custom Input)
- Responsive: mobile-first, but desktop layout is the primary target for this app

## Loading & Error States
- Loading states: Skeleton loaders (from shadcn/ui) for tables and lists — not generic spinners
- Chat streaming: animated three-dot `StreamingDots.tsx` while response is generating
- Error states: every `useQuery` hook surfaces errors via a shared `ErrorAlert` component
- Empty states: every list shows a clear empty state message (not a blank space)

## Chat Streaming
- Use `useStreamingChat.ts` hook; it opens an `EventSource` to `/api/chat/sessions/{id}/stream`
- Tokens appended to local state as they arrive; React Query cache updated on stream close
- Abort ongoing stream when user navigates away (`useEffect` cleanup closes EventSource)

## TypeScript
- Strict mode enabled (`"strict": true` in tsconfig)
- No `any` — use `unknown` and narrow, or define proper types
- API response shapes typed in `src/types/`; match backend DTO field names exactly

## React Query Config
- `staleTime: 30_000` for document lists (30s before refetch)
- `refetchInterval: 3000` for documents with status PROCESSING (poll until INDEXED)
- Invalidate document list cache on successful upload mutation

## Routing
- React Router v6; routes defined in `App.tsx`
- Protected routes: redirect to `/login` if no JWT in Zustand store
- Route map: `/login`, `/upload`, `/chat` (default redirect to `/upload` after login)

## Forms
- Use `react-hook-form` + `zod` for form validation
- Never manipulate form state manually with `useState`

## Accessibility
- All interactive elements have accessible labels (`aria-label` or visible text)
- Color contrast: text must meet WCAG AA against dark backgrounds
- Keyboard navigable: Tab order follows visual order
