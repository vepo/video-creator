# Design System — Video Creator

Visual identity inspired by Kdenlive / KDE Breeze Dark. Professional non-linear editor aesthetic — dark, low-glare, flat panels.

## Brand

| Item | Value |
|------|-------|
| Product name | **Video Creator** |
| Tagline | Non-linear video editor |
| Voice | Professional, precise, tool-focused — not playful or marketing-heavy |

## Reference (mood, not assets)

Kdenlive traits we adopt:

- Dark workspace for long editing sessions
- Flat panels with 1px separators
- KDE highlight blue for primary actions
- Warm orange selection on timeline items
- Red playhead
- Blue video clips, red/coral audio clips
- Compact toolbar controls (32–40px)

Do **not** copy Kdenlive logo, screenshots, or Breeze icon font files.

## Color tokens

All colors are defined as CSS custom properties in [`theme.css`](../src/main/resources/META-INF/resources/css/theme.css). Use `var(--vc-*)` — never hardcode hex in page stylesheets.

| Token | Hex | Use |
|-------|-----|-----|
| `--vc-color-bg-app` | `#232629` | Page background |
| `--vc-color-bg-panel` | `#2a2e32` | Panel body |
| `--vc-color-bg-panel-alt` | `#31363b` | Panel headers, inputs |
| `--vc-color-border` | `#3d4348` | Separators |
| `--vc-color-text` | `#eff0f1` | Primary text |
| `--vc-color-text-muted` | `#bdc3c7` | Labels, secondary |
| `--vc-color-accent` | `#3daee9` | Primary buttons, active tabs |
| `--vc-color-selection` | `#f39c12` | Selected clip/media border |
| `--vc-color-playhead` | `#e74c3c` | Timeline playhead |
| `--vc-color-clip-video` | `#2980b9` | Video track clips |
| `--vc-color-clip-audio` | `#c0392b` | Audio track clips |
| `--vc-color-success` | `#27ae60` | OK status |
| `--vc-color-error` | `#e74c3c` | Error status |

## Typography

Font stack: **Noto Sans** (Google Fonts) with system-ui fallback.

| Token | Size | Use |
|-------|------|-----|
| `--vc-font-size-xs` | 11px | Clip names, ruler marks |
| `--vc-font-size-sm` | 12px | Status values, durations |
| `--vc-font-size-base` | 13px | Form labels, property fields |
| `--vc-font-size-md` | 14px | Body, buttons, panel titles |
| `--vc-font-size-lg` | 18px | Section headings |
| `--vc-font-size-xl` | 24px | App title |

## Spacing

| Token | Value |
|-------|-------|
| `--vc-space-xs` | 4px |
| `--vc-space-sm` | 8px |
| `--vc-space-md` | 12px |
| `--vc-space-lg` | 16px |
| `--vc-space-xl` | 24px |

## Components

### Buttons

- `.btn` — base
- `.btn-primary` — accent blue (New Project, Export)
- `.btn-secondary` — neutral panel button
- `.btn-sm` — compact table actions

### Panels

- `.panel` + `.panel-header` + `.panel-body`
- Used on main window (Projects, System Status) and editor side panels

### Status rows

- `.status-row` + `.status-row--ok` / `.status-row--error`
- Dot indicator + label + value (OK / Unavailable)

### Icons

SVG sprite at `/icons/icons.svg`. Usage:

```html
<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#video"/></svg>
```

**No emoji in UI chrome.** Use sprite icons for media types, transport, and actions.

## Page structure

### Main window (`/`)

Three zones:

1. **Header** — logo + Video Creator
2. **Projects panel** — list + New Project button
3. **System Status panel** — MLT, video engine, database

### Editor (`/editor/{id}`)

Three-column NLE layout: project files | preview + timeline | properties.

## Stylesheet load order

```html
<link rel="stylesheet" href="/css/theme.css">
<link rel="stylesheet" href="/css/style.css">   <!-- or editor.css -->
```

## Do / Don't

| Do | Don't |
|----|-------|
| Use `--vc-*` tokens | Hardcode colors in page CSS |
| SVG icons from sprite | Emoji in buttons, headers, status |
| Flat 1px borders | Heavy shadows, gradients |
| Panel-based layout | Marketing feature cards on home |
| Match editor + home palette | Light theme on home, dark on editor |

## Assets

| Path | Purpose |
|------|---------|
| `/brand/logo.svg` | Full wordmark |
| `/brand/logo-icon.svg` | Header icon |
| `/favicon.svg` | Browser tab |
| `/icons/icons.svg` | Icon sprite |
