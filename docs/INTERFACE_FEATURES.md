# Interface Features

Prototype status of the web UI (`index.html`, `editor.html`, `index.js`, `editor.js`). A feature is **implemented** when the interface exposes working behavior (not merely static markup or placeholder controls).

**Summary:** 70 implemented · 0 not implemented · 0 partial

> **Note:** Transitions and effects are stored on clips and exposed in the UI; MLT render integration for transition/effect filters is not yet wired in `TimelineAssembler`. For a full **Kdenlive parity** roadmap (implemented / partial / missing), see [`KDENLIVE_FEATURES.md`](KDENLIVE_FEATURES.md).

---

## Main Window (`/`)

Three functional zones: header, projects panel, system status panel. Kdenlive-inspired dark theme. See [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

| Status | Feature |
|--------|---------|
| - [x] | Application header with Video Creator branding and logo |
| - [x] | Projects panel with table list (name, created, media count) |
| - [x] | New Project primary action (always visible in panel header) |
| - [x] | Open existing project from table row or Open button |
| - [x] | Empty state when no projects exist |
| - [x] | Show project creation date (formatted via `index.js`) |
| - [x] | System status panel — MLT (melt) |
| - [x] | System status panel — video engine |
| - [x] | System status panel — database connection |
| - [x] | Health API refresh for MLT status (`index.js`) |
| - [x] | Application version in status panel |
| - [x] | Delete a project |
| - [x] | Edit project metadata from the main window (rename) |
| - [x] | Search or filter projects |

---

## Editor — Shell & Navigation (`/editor/{id}`)

| Status | Feature |
|--------|---------|
| - [x] | Load editor with embedded project JSON (`currentProject`) |
| - [x] | Editor header with Video Creator logo and title |
| - [x] | Navigate back to the main window (logo links to `/`) |
| - [x] | File menu actions (New, Open, Save, Export) |
| - [x] | Edit menu actions (Cut, Delete, Deselect) |
| - [x] | View menu actions (Zoom in/out, Fit timeline) |
| - [x] | Help menu actions (About) |

---

## Editor — Media Library (left panel)

| Status | Feature |
|--------|---------|
| - [x] | Project files panel layout |
| - [x] | Drop zone with drag-over highlight |
| - [x] | Drag-and-drop file upload to the server (`MediaLibrary.add` → `POST /api/editor/{id}/media`) |
| - [x] | Refresh media list after upload (`UI.reconciliateMedias`) |
| - [x] | Display media file name and duration |
| - [x] | Media type icons (video / audio / image / unknown) |
| - [x] | Select a media item to show properties |
| - [x] | Drag media from the library onto the timeline |
| - [x] | Upload button opens file picker and uploads media |
| - [x] | Remove media from the project |
| - [x] | Rename media |

---

## Editor — Timeline (center panel)

| Status | Feature |
|--------|---------|
| - [x] | Timeline layout with video and audio tracks |
| - [x] | Drop video/image media onto the video track |
| - [x] | Drop audio/video media onto the audio track |
| - [x] | Shadow clip preview while dragging media onto a track |
| - [x] | Create timeline clips after drop (`Project.addMediaToTimeline`) |
| - [x] | Render clips on the timeline (`UI.reconciliateClips`) |
| - [x] | Select a clip to show properties |
| - [x] | Timeline zoom in |
| - [x] | Timeline zoom out |
| - [x] | Fit timeline to project duration |
| - [x] | Ctrl + mouse wheel zoom |
| - [x] | Zoom level indicator (`Zoom: N%`) |
| - [x] | Horizontal timeline scroll via scroll handle |
| - [x] | Playhead position timecode (`#playheadTimeLabel`) |
| - [x] | Timeline ruler with readable time marks |
| - [x] | Drag clip to reposition on the timeline |
| - [x] | Trim clip in/out points via edge drag handles |
| - [x] | Cut at playhead (video + synced audio) |
| - [x] | Delete clip from the timeline |
| - [x] | Edit clip via inline controls (properties / delete on clip) |
| - [x] | Add or remove tracks |
| - [x] | Mute track |
| - [x] | Lock track |
| - [x] | Interactive playhead scrubbing |

---

## Editor — Preview (center panel)

| Status | Feature |
|--------|---------|
| - [x] | Preview area placeholder |
| - [x] | Play video in the preview panel |
| - [x] | Header Preview button |
| - [x] | Header Stop button |
| - [x] | Preview transport controls (⏮ ⏪ ▶ ⏩ ⏭) |
| - [x] | Request timeline preview from API (`POST /api/editor/{id}/preview`) |

---

## Editor — Properties (right panel)

| Status | Feature |
|--------|---------|
| - [x] | Properties tab (default) |
| - [x] | Empty state when nothing is selected |
| - [x] | Media properties (name editable, duration read-only) |
| - [x] | Clip properties panel (name, start, duration, speed) |
| - [x] | Edit project name (persisted via auto-save) |
| - [x] | Edit project description |
| - [x] | Select screen size / resolution (persisted) |
| - [x] | Select frame rate (persisted) |
| - [x] | Edit clip name and persist changes |
| - [x] | Edit clip start time and persist changes |
| - [x] | Edit clip speed and persist changes |
| - [x] | Edit project duration |
| - [x] | Save project settings to the server |
| - [x] | Transitions tab content (apply to selected clip) |
| - [x] | Effects tab content (apply to selected clip) |
| - [x] | Tab switching between Properties / Transitions / Effects |

---

## Editor — Export

| Status | Feature |
|--------|---------|
| - [x] | Open export modal from **Export Video** header button |
| - [x] | Close export modal (× and backdrop click) |
| - [x] | Choose export format (MP4 / WebM / MOV) — functional |
| - [x] | Choose export quality (High / Medium / Low) — functional |
| - [x] | Export progress bar and status text |
| - [x] | Start render from modal (`POST /api/editor/{id}/render`) |
| - [x] | Download rendered file from the UI (`GET /download/{filename}`) |

---

## Cross-cutting / persistence

| Status | Feature |
|--------|---------|
| - [x] | Create project on first visit to `/editor/new` |
| - [x] | Upload media and persist to MongoDB / GridFS |
| - [x] | Persist timeline clips to the server |
| - [x] | Persist project name, description, screen size, frame rate, duration edits |
| - [x] | Auto-save (debounced) and explicit Save action |
| - [x] | Error toast / notification system (`UI.notify` → `#editorStatus`) |

---

## API endpoints used by the UI

| Status | Endpoint | UI usage |
|--------|----------|----------|
| - [x] | `GET /` | Home page |
| - [x] | `GET /editor/{id}` | Editor page |
| - [x] | `PUT /api/editor/{projectId}` | Save project (`ProjectSave.save`) |
| - [x] | `DELETE /api/editor/{projectId}` | Delete project (main window) |
| - [x] | `PUT /api/projects/{projectId}` | Rename project metadata (main window) |
| - [x] | `POST /api/editor/{projectId}/media` | Media upload |
| - [x] | `DELETE /api/editor/{projectId}/media/{hash}` | Remove media |
| - [x] | `PUT /api/editor/{projectId}/media/{hash}` | Rename media |
| - [x] | `POST /api/editor/{projectId}/preview/session` | HLS preview session (replaces file-based preview) |
| - [x] | `DELETE /api/editor/{projectId}/preview/session/{sessionId}` | Stop preview session |
| - [x] | `GET /preview/{sessionId}/{path}` | HLS manifest and segments |
| - [x] | `POST /api/editor/{projectId}/render` | Export render (format + quality) |
| - [x] | `POST /api/editor/{projectId}/render/queue` | Enqueue render job |
| - [x] | `POST /api/editor/{projectId}/duplicate` | Duplicate project |
| - [x] | `GET /api/templates` | Project templates |
| - [x] | `GET /api/editor/{projectId}/archive` | Project archive download |
| - [x] | `GET /download/{filename}` | Export download |
| - [x] | `GET /api/video/health` | Polled from main window |
