# Nettdetektivene - Frontend Styling Guide

Reference for developers adding or changing UI. Read this before writing any CSS or reaching for Tailwind.

---

## 1. Core Aesthetic

The app has two distinct visual layers:

- **Student views** - game/visual-novel feel: full-screen backgrounds, SceneComponent wrapper, positioned game elements, paper notes, scanlines. Avoid Tailwind here.
- **Teacher views** - polished admin feel: glass panels, notched cards/buttons, split layouts. Tailwind is acceptable for layout utilities, but scoped CSS is preferred for anything non-trivial.

Both layers share the same **pixel-notch button system** and **CSS custom properties** defined in `src/assets/main.css`.

---

## 2. CSS Custom Properties

All colors and geometry live as `@theme` tokens in `main.css`. Never hardcode these values in components.

### Color tokens

| Token | Value | Use |
|---|---|---|
| `--color-nd-navy` | `#1a2744` | Primary dark background |
| `--color-nd-navy-light` | `#243460` | Secondary accent/hover |
| `--color-nd-accent` | `#f5a623` | Orange CTA, highlights |
| `--color-nd-accent-hover` | `#e09518` | Accent hover state |
| `--color-nd-success` | `#22c55e` | Completion, approval |
| `--color-nd-danger` | `#ef4444` | Destructive, errors |
| `--color-nd-muted` | `#6b7280` | Placeholder, disabled text |
| `--color-nd-glow` | `#f0c040` | Focus rings (golden) |
| `--color-nd-glow-cool` | `#6cd1ff` | Alternative focus (cyan) |
| `--color-nd-panel` | `rgba(10,10,20,0.92)` | Glass dialog/card background |
| `--color-nd-panel-border` | `rgba(255,255,255,0.18)` | Glass card border |
| `--color-nd-vignette` | `rgba(0,0,0,0.55)` | Scene vignette overlay |

### Button palette tokens

| Token | Value |
|---|---|
| `--color-nd-button` | `#ffffff` (fill) |
| `--color-nd-button-hover` | `#111111` (hover fill) |
| `--color-nd-button-edge` | `#111111` (outline) |
| `--color-nd-button-text` | `#111111` |
| `--color-nd-button-text-hover` | `#ffffff` |
| `--color-nd-button-danger-edge` | `#b91c1c` |
| `--color-nd-button-danger-text` | `#b91c1c` |

### Geometry tokens

```css
--radius-card: 0.75rem
--notch-clip: polygon(8px 0, 100% 0, 100% calc(100% - 18px), calc(100% - 18px) 100%, 0 100%, 0 8px)
```

The notch clip cuts the top-left and bottom-right corners of elements to create a pixel-art silhouette. Apply to cards and containers via `clip-path: var(--notch-clip)`.

---

## 3. Button System

Every `<button>` automatically gets the global pixel-notch style (white fill, black outline, inverts on hover) via `main.css`. You do not need to add classes for the default button.

### Variants

```html
<!-- Default: white fill, black edge, inverts on hover -->
<button>Lagre</button>

<!-- Destructive: red outline and text, does NOT invert -->
<button class="btn-vn-danger">Slett</button>

<!-- Ghost: transparent fill, white text for use on dark backgrounds -->
<button class="btn-vn-ghost">Avbryt</button>

<!-- Opt-out: removes all global button styles. Use for icon buttons,
     toggle switches, quiz cards, or anything with fully custom visuals. -->
<button class="btn-bare">...</button>
```

### Rules

- Use `btn-vn-danger` for destructive actions (delete, reject, remove).
- Use `btn-vn-ghost` for secondary/cancel actions on dark backgrounds.
- Use `btn-bare` when the global notch shape would break your component (e.g. star-rating buttons, image upload triggers, toggle chips). Apply your own hover/focus styles.
- Never override `::before`/`::after` on a non-bare button - you will break the notch edge highlight.
- Minimum tap target: `44px` height. The global button padding gives `~46px` by default. If you are using `btn-bare` with compact sizing, add `min-height: 44px` explicitly.

### Sizing helpers

```css
/* Global helpers defined in main.css */
.overview-primary-btn  /* Full-width, 1.15rem font, large padding */
.overview-action-btn   /* Inline action, 0.95rem font, compact padding */
.btn-stack             /* Flex-column container for button lists */
```

---

## 4. Layout Patterns

### SceneComponent (required wrapper for all views)

Every route view must be wrapped in `SceneComponent`. It provides the full-screen background, the semi-transparent overlay, the vignette, the navigation header, and the scroll container.

```vue
<SceneComponent
  title="Page Title"
  background="/MyBackground.jpg"
  :previous="backRoute"
>
  <!-- your content fills .scene-body -->
</SceneComponent>
```

Props: `title` (required), `previous`, `next`, `background`, `vignette`, `scanlines`, `dimOverlay`.

### Two-column teacher layout

```html
<div class="scene-split-layout">
  <div class="scene-split-grid">
    <main class="scene-split-main"><!-- primary content --></main>
    <aside class="scene-split-side"><!-- side menu/panel --></aside>
  </div>
</div>
```

Collapses to a single column at `950px` (side moves above main). Do not add custom media queries that fight this - extend `.scene-split-grid` if you need a different side-panel width.

### Glass panel

```html
<div class="vn-panel">...</div>
```

`rgba(10,10,20,0.92)` background, `backdrop-filter: blur(6px)`, `0.75rem` radius. Use for dialogs, floating cards, and task quiz containers.

### Row list (teacher admin lists)

Classroom rows and student rows use:
- Container: `background: rgba(0,0,0,0.35)`, `border: 1px solid rgba(255,255,255,0.08)`, `border-radius: 0.875rem`, `backdrop-filter: blur(4px)`
- Row items: `background: rgba(255,255,255,0.95)`, `clip-path: var(--notch-clip)`
- Row entry animation: `slideUp` keyframes with staggered `--animation-delay`

---

## 5. Typography

### Headings

Global styles in `main.css` apply text stroke to all headings automatically:

- `h1`-`h3`: `-webkit-text-stroke: 5px #000000`, `paint-order: stroke fill`, `color: #ffffff`
- `h4`-`h6`: `-webkit-text-stroke: 3px #000000`

Do not redeclare these in scoped styles unless you have a specific reason to override the stroke (e.g. a heading on a white card needs `-webkit-text-stroke: 0`).

### Fluid type

Prefer `clamp()` over breakpoint-based font changes for headings that need to scale:

```css
font-size: clamp(1.4rem, 2vw, 2rem); /* min 1.4rem, scales, max 2rem */
```

### Body copy on dark backgrounds

White with a subtle shadow to ensure legibility over varied background images:

```css
color: #ffffff;
text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
```

### Form labels

```css
color: white;
font-weight: 800;
font-size: 0.9rem;
text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8);
```

---

## 6. Responsive Design

### Breakpoints

| px | Use |
|---|---|
| `480px` | Small phones (375px standard, 320px minimum) |
| `720px` | Large phones / small tablets, stacks multi-column forms to single column |
| `950px` | Tablet - collapses the `scene-split-grid` to single column (defined in `main.css`) |
| `1100px` | Wide desktop adjustments for photo-grid layouts |

### Rules

1. Write base styles for mobile first, then add wider breakpoints with `@media (min-width: ...)` or use the override pattern `@media (max-width: ...)` as the project currently does - **be consistent within a file**.
2. For student game views (GameMapView, TeacherTestGameView), map objects use percentage widths and absolute positioning - adjust `width` and label `font-size` in narrow breakpoints rather than switching to flex/grid layout.
3. For teacher form views (CreateTaskView, EditClassroomTaskView), stack card grids to 1 column at `720px`. Reduce outer padding at `480px`.
4. For Tailwind-heavy views (NotificationView, ClassroomStudentsView), use `sm:` prefixes (`sm:px-6`, `sm:w-auto`, `sm:flex-row`) rather than scoped media queries.

### Touch targets

All interactive elements must be at least `44x44px` to meet WCAG 2.5.5. The default global button meets this. For custom `btn-bare` elements (rating buttons, icon toggles), add:

```css
.my-btn {
  min-height: 44px;
  min-width: 44px;
}
```

---

## 7. When to Use Scoped CSS vs Tailwind

| Situation | Use |
|---|---|
| Student game views (map, stops, home board) | Scoped CSS only - precise positioning, animations, paper aesthetics |
| Teacher form views (create/edit tasks) | Scoped CSS - complex forms with custom upload cards |
| Teacher list/panel views | Scoped CSS for rows and containers, Tailwind for quick spacing |
| Simple teacher views with flat card layouts | Tailwind is fine |
| Overriding global component styles | Scoped CSS always |
| Responsive layout utilities in Tailwind files | `sm:` / `md:` prefixes - do not mix scoped media queries in the same file |

**Never mix scoped `@media` blocks and Tailwind responsive prefixes in the same file.** Pick one approach per file and stay consistent.

---

## 8. Animations

### Standard transitions

```css
transition: background-color 0.15s ease, color 0.15s ease, transform 0.12s ease;
```

Hover scale: `transform: scale(1.04)`. Active press: `transform: scale(0.98)`.

### Entry animations

```css
@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes slideRight {
  from { opacity: 0; transform: translateX(12px); }
  to   { opacity: 1; transform: translateX(0); }
}
```

Use `animation: slideUp 0.4s ease-out both` on list items. Stagger via inline style:

```vue
<div
  v-for="(item, i) in items"
  :style="{ animationDelay: `${i * 0.05}s` }"
/>
```

### Reduced motion

Always wrap animations that are purely decorative in a `prefers-reduced-motion` guard:

```css
@media (prefers-reduced-motion: reduce) {
  .my-animated-element {
    animation: none;
    transition: none;
  }
}
```

---

## 9. Accessibility

- **Focus rings**: Use `outline: 2px solid var(--color-nd-glow); outline-offset: 3px` on all interactive elements. This is applied globally by `main.css` - do not remove it with `outline: none`.
- **Color contrast**: White text on the semi-transparent overlay meets WCAG AA. Avoid light gray text on white backgrounds.
- **ARIA**: Add `aria-label` to icon-only buttons. Use `role="button"` and `tabindex="0"` on non-button interactive elements.
- **Touch targets**: 44px minimum (see section 6).
- **Keyboard navigation**: Ensure every interactive element is reachable and operable by keyboard. Test with Tab and Enter/Space.

---

## 10. Class Naming Conventions

Follow the pattern used across the codebase:

- **View-scoped classes**: `kebab-case`, prefixed with the component context: `.task-create-page`, `.student-row`, `.map-object`
- **Modifier classes**: `--modifier` suffix: `.task-upload-card--good`, `.diff-btn--active`, `.map-object--locked`
- **State classes**: `--state` suffix: `.diff-btn--active`, `.task-btn--completed`
- **Utility wrappers**: Use global classes from `main.css` when available (`.vn-panel`, `.menu-stack`, `.btn-stack`) instead of re-implementing them locally.

---

## 11. Scoped CSS File Structure

Organize `<style scoped>` blocks in this order:

```css
/* 1. Container / page wrapper */
.view-name { ... }

/* 2. Layout children (header, body, footer sections) */
.view-name__section { ... }

/* 3. Card / list components */
.card { ... }
.card--modifier { ... }

/* 4. Individual elements */
.element { ... }
.element--modifier { ... }

/* 5. Interactive states */
.element:hover { ... }
.element:focus-visible { ... }

/* 6. Responsive breakpoints - widest first, narrowest last */
@media (max-width: 1100px) { ... }
@media (max-width: 950px)  { ... }
@media (max-width: 720px)  { ... }
@media (max-width: 480px)  { ... }
```
