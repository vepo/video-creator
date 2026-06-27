# Interface Features

Prototype status of the web UI (`index.html`, `editor.html`, `index.js`, `editor.js`). A feature is **implemented** when the interface exposes working behavior (not merely static markup or placeholder controls).

**Summary:** 28 implemented · 41 not implemented · 3 partial

---

## Home Page (`/`)

| Status | Feature |
|--------|---------|
| - [x] | Application header and branding |
| - [x] | MLT availability status (success / error banner) |
| - [x] | List projects loaded from the database |
| - [x] | Open an existing project (navigate to editor) |
| - [x] | Create a new project (`/editor/new` redirect) |
| - [x] | Show project name on each project card |
| - [x] | Show project description on each project card |
| - [x] | Show project creation date (formatted from epoch via `index.js`) |
| - [ ] | Delete a project |
| - [ ] | Edit project metadata from the home page |
| - [ ] | Search or filter projects |

### Advertised on home page (marketing cards only)

These appear under **Features** on the home page but have no working UI behind them yet:

| Status | Feature |
|--------|---------|
| - [ ] | Multi-track timeline editor (beyond the basic editor prototype) |
| - [ ] | Trim and cut videos with drag-and-drop precision |
| - [ ] | Audio mixing across multiple tracks |
| - [ ] | Live preview before rendering |
| - [ ] | Multiple export formats and codecs (user-facing) |
| - [ ] | Fast MLT-powered processing (user-facing workflow) |

---

## Editor — Shell & Navigation (`/editor/{id}`)

| Status | Feature |
|--------|---------|
| - [x] | Load editor with embedded project JSON (`currentProject`) |
| - [x] | Editor header with logo and title |
| - [ ] | File menu actions |
| - [ ] | Edit menu actions |
| - [ ] | View menu actions |
| - [ ] | Help menu actions |
| - [ ] | Navigate back to the home page |

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
| - [x] | Select a media item to show properties (read-only) |
| - [x] | Drag media from the library onto the timeline |
| - [ ] | Upload button (`+`) opens a file picker |
| - [ ] | Remove media from the project |
| - [ ] | Rename media |

---

## Editor — Timeline (center panel)

| Status | Feature |
|--------|---------|
| - [x] | Timeline layout with video and audio tracks |
| - [x] | Drop video/image media onto the video track |
| - [x] | Drop audio/video media onto the audio track |
| - [x] | Shadow clip preview while dragging media onto a track |
| - [x] | Create timeline clips after drop (`Project.addClipMedia`) |
| - [x] | Render clips on the timeline (`UI.reconciliateClips`) |
| - [x] | Select a clip to show properties |
| - [x] | Timeline zoom in (`🔍+`) |
| - [x] | Timeline zoom out (`🔍-`) |
| - [x] | Fit timeline to project duration (`📐 Fit`) |
| - [x] | Ctrl + mouse wheel zoom |
| - [x] | Zoom level indicator (`Zoom: N%`) |
| - [x] | Horizontal timeline scroll via scroll handle |
| - [ ] | Timeline ruler with readable time marks *(partial: marks render as placeholder `x`)* |
| - [ ] | Drag clip to reposition on the timeline *(partial: drag handlers log only)* |
| - [ ] | Trim clip in/out points on the timeline |
| - [ ] | Delete clip from the timeline |
| - [ ] | Edit clip via inline controls (✏️ / 🗑️) |
| - [ ] | Add or remove tracks |
| - [ ] | Mute track (`🔇`) |
| - [ ] | Lock track (`🔒`) |
| - [ ] | Interactive playhead scrubbing *(playhead is static markup)* |

---

## Editor — Preview (center panel)

| Status | Feature |
|--------|---------|
| - [x] | Preview area placeholder |
| - [ ] | Play video in the preview panel |
| - [ ] | Header **Play** button |
| - [ ] | Header **Stop** button |
| - [ ] | Preview transport controls (⏮ ⏪ ▶ ⏩ ⏭) |
| - [ ] | Request timeline preview from API (`POST /api/timeline/preview`) |

---

## Editor — Properties (right panel)

| Status | Feature |
|--------|---------|
| - [x] | Properties tab (default) |
| - [x] | Empty state when nothing is selected |
| - [x] | Media properties (name, duration — read-only) |
| - [x] | Clip properties panel (name, start, duration, speed — displayed) |
| - [x] | Edit project name *(in-memory only)* |
| - [x] | Select screen size / resolution *(in-memory only)* |
| - [x] | Select frame rate *(in-memory only)* |
| - [ ] | Edit clip name and persist changes |
| - [ ] | Edit clip start time and persist changes |
| - [ ] | Edit clip speed and persist changes |
| - [ ] | Edit project duration *(partial: controls render but do not update `currentProject.duration`)* |
| - [ ] | Edit project description |
| - [ ] | Save project settings to the server |
| - [ ] | Transitions tab content |
| - [ ] | Effects tab content |
| - [ ] | Tab switching between Properties / Transitions / Effects *(partial: click handler references undefined `e`)* |

---

## Editor — Export

| Status | Feature |
|--------|---------|
| - [x] | Open export modal from **Export Video** header button |
| - [x] | Close export modal (× and backdrop click) |
| - [ ] | Choose export format (MP4 / WebM / MOV) — functional |
| - [ ] | Choose export quality (High / Medium / Low) — functional |
| - [ ] | Export progress bar and status text — functional |
| - [ ] | Start render from modal (**Export** button → `POST /api/timeline/render`) |
| - [ ] | Download rendered file from the UI (`GET /download/{filename}`) |

---

## Cross-cutting / persistence

| Status | Feature |
|--------|---------|
| - [x] | Create project on first visit to `/editor/new` |
| - [x] | Upload media and persist to MongoDB / GridFS |
| - [ ] | Persist timeline clips to the server |
| - [ ] | Persist project name, screen size, frame rate, duration edits |
| - [ ] | Auto-save or explicit save action |
| - [ ] | Error toast / notification system (uses `alert()` for upload errors only) |

---

## API endpoints used by the UI

| Status | Endpoint | UI usage |
|--------|----------|----------|
| - [x] | `GET /` | Home page |
| - [x] | `GET /editor/{id}` | Editor page |
| - [x] | `POST /api/editor/{projectId}/media` | Media upload from drag-and-drop |
| - [ ] | `POST /api/timeline/preview` | Not called from `editor.js` |
| - [ ] | `POST /api/timeline/render` | Not called from `editor.js` |
| - [ ] | `GET /download/{filename}` | No download link in the UI |
| - [ ] | `GET /api/video/health` | Not called from the UI (status shown via server render on home) |

---

## Quick checklist (all interface features)

### Home
- [x] Application header and branding
- [x] MLT availability status
- [x] List projects from database
- [x] Open existing project
- [x] Create new project
- [x] Project card name
- [x] Project card description
- [x] Project card creation date formatting
- [ ] Delete project
- [ ] Edit project from home
- [ ] Search / filter projects

### Editor — navigation & shell
- [x] Load project into editor
- [x] Editor header / logo
- [ ] File / Edit / View / Help menus
- [ ] Back to home navigation

### Editor — media library
- [x] Project files panel
- [x] Drop zone highlight
- [x] Drag-and-drop upload
- [x] Media list after upload
- [x] Media icons and duration
- [x] Select media properties
- [x] Drag media to timeline
- [ ] Upload button file picker
- [ ] Remove media
- [ ] Rename media

### Editor — timeline
- [x] Video and audio tracks
- [x] Drop media onto video track
- [x] Drop media onto audio track
- [x] Drag shadow preview
- [x] Create clips on drop
- [x] Display clips on timeline
- [x] Select clip properties
- [x] Zoom in / out / fit
- [x] Ctrl + wheel zoom
- [x] Zoom indicator
- [x] Timeline horizontal scroll
- [ ] Readable time ruler
- [ ] Reposition clips by drag
- [ ] Trim clips
- [ ] Delete clips
- [ ] Inline clip edit controls
- [ ] Add / remove tracks
- [ ] Track mute
- [ ] Track lock
- [ ] Playhead scrubbing

### Editor — preview
- [x] Preview placeholder
- [ ] Video playback
- [ ] Header play / stop
- [ ] Preview transport bar
- [ ] Timeline preview API integration

### Editor — properties
- [x] Properties tab
- [x] Empty selection state
- [x] Media properties (read-only)
- [x] Clip properties display
- [x] Project name (in-memory)
- [x] Screen size selector (in-memory)
- [x] Frame rate selector (in-memory)
- [ ] Persist clip property edits
- [ ] Project duration editor
- [ ] Project description
- [ ] Save to server
- [ ] Transitions tab
- [ ] Effects tab
- [ ] Working tab switcher

### Editor — export
- [x] Open export modal
- [x] Close export modal
- [ ] Export format selection
- [ ] Export quality selection
- [ ] Export progress
- [ ] Start render
- [ ] Download result

### Advertised (home marketing only)
- [ ] Multi-track timeline (full)
- [ ] Trim and cut
- [ ] Audio mixing
- [ ] Live preview
- [ ] Multiple formats
- [ ] Fast processing workflow
