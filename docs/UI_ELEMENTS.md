# UI Elements Catalog

Authoritative inventory of every user-facing control, region, and dynamically rendered element in the web UI.

**Related docs:** [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) (visual tokens), [`INTERFACE_FEATURES.md`](INTERFACE_FEATURES.md) (feature implementation status).

**Sources:** `templates/index.html`, `templates/editor.html`, `javascript/index.js`, `javascript/editor.js`, `css/theme.css`, `css/style.css`, `css/editor.css`, `icons/icons.svg`.

---

## How to maintain this file

When you **add, rename, remove, or materially change** any UI element, update this catalog in the **same change** — before finishing the task.

Each entry must include:

| Field | Description |
|-------|-------------|
| **Name** | Human-readable label |
| **Selector** | Stable `id`, class, or attribute hook (`item-hash`, etc.) |
| **Page** | `main` (`/`) or `editor` (`/editor/{id}`) or `shared` |
| **Purpose** | What the user sees or does |
| **Behavior** | Wired handler, API call, or static display |
| **Source** | Template, JS namespace, or CSS file |

---

## Shared components (`theme.css`)

| Name | Selector | Page | Purpose | Behavior | Source |
|------|----------|------|---------|----------|--------|
| Primary button | `.btn.btn-primary` | shared | Main call to action | Click / link navigation | `theme.css` |
| Secondary button | `.btn.btn-secondary` | shared | Secondary action | Click | `theme.css` |
| Small button | `.btn.btn-sm` | shared | Compact toolbar/table action | Click | `theme.css` |
| Icon button | `.btn.btn-icon` | editor | Icon-only timeline control | Click | `editor.css` |
| Panel | `.panel` + `.panel-header` + `.panel-body` | main | Grouped content region | — | `theme.css` |
| Icon | `.icon`, `.icon-lg`, `.icon-xl` | shared | SVG sprite glyph | Decorative (`aria-hidden`) | `theme.css` |
| Status row | `.status-row`, `.status-row--ok`, `.status-row--error` | main | Service health line | Server-rendered; JS may refresh | `theme.css` |
| Text input | `input[type="text"]` | editor | Property fields | Change → project state | `theme.css` |
| Number input | `input[type="number"]` | editor | Numeric property / duration | Spin buttons added by `UI.setupDurationController` | `theme.css`, `editor.js` |
| Select | `select` | editor | Enumerated settings | Change → project state | `theme.css` |
| Visually hidden | `.visually-hidden` | editor | Screen-reader / programmatic only | Hidden from layout | `editor.css` |
| Editor status bar | `#editorStatus`, `.editor-status--info/success/error` | editor | Global feedback (save, upload, errors) | `UI.notify()` — `aria-live="polite"` | `editor.html`, `editor.js` |
| Tab coming soon | `#tabComingSoon`, `.tab-coming-soon` | editor | Placeholder for unimplemented tabs | Shown when Transitions/Effects tab selected | `editor.html`, `editor.js` |

---

## Icon sprite (`/icons/icons.svg`)

| Symbol ID | Used for |
|-----------|----------|
| `video` | Video media, preview placeholder |
| `audio` | Audio media |
| `image` | Image media |
| `unknown` | Unrecognized media type |
| `folder` | Projects / project files panel |
| `project` | Project row, New Project |
| `play` | Preview / transport |
| `stop` | Stop preview |
| `export` | Save, export actions |
| `upload` | Upload media, drop zone |
| `mute` | Track mute toggle |
| `lock` | Track lock toggle |
| `zoom-in` | Timeline zoom in |
| `zoom-out` | Timeline zoom out |
| `fit` | Fit timeline to content |
| `status-ok` | System status panel header |
| `status-error` | (available; not yet used in templates) |

---

## Main window — `/` (`index.html`, `index.js`)

### Header

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| App header | `.app-header` | Branding bar | — | `index.html` |
| Brand link | `.app-header-brand` | Logo + title | Navigates to `/` | `index.html` |
| App title | `.app-header-title` | "Video Creator" | Static text | `index.html` |
| Subtitle | `.app-header-subtitle` | Tagline | Static text | `index.html` |

### Projects panel

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Projects panel | `.projects-panel` | Project list region | — | `index.html` |
| New Project (header) | `.panel-header-actions .btn-primary` | Create project | Link → `/editor/new` | `index.html` |
| Empty state | `.empty-state` | No projects message | Shown when list empty; includes New Project link | `index.html` |
| Project table | `.project-table` | Project rows | — | `index.html` |
| Project name link | `.project-name a` | Open project | Link → `/editor/{id}` | `index.html` |
| Created date | `.col-created.epoch-milli` | Creation timestamp | Formatted by `index.js` on load | `index.html`, `index.js` |
| Media count | `.col-media` | Number of media items | Server-rendered count | `index.html` |
| Open button | `.col-action .btn-secondary` | Open project | Link → `/editor/{id}` | `index.html` |

### System status panel

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Status panel | `.status-panel` | Health summary | — | `index.html` |
| MLT status | `#status-mlt` | Melt availability | Server-rendered; refreshed via `GET /api/video/health` | `index.html`, `index.js` |
| Video engine | `#status-engine` | Render engine ready | Same health poll | `index.html`, `index.js` |
| Database | `#status-db` | MongoDB connection | Server-rendered only | `index.html` |
| App version | `.status-version` | Version string | Static from server | `index.html` |

---

## Editor — `/editor/{id}` (`editor.html`, `editor.js`)

### Header

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Editor header | `.editor-header` | Top toolbar | — | `editor.html` |
| Back to projects | `.logo` | Return to main window | Link → `/` | `editor.html` |
| Menu bar | `.menu-bar` | File / Edit / View / Help | Placeholder — `.menu-item--disabled`, title "Coming soon" | `editor.html` |
| Save | `#saveBtn` | Persist project | `ProjectSave.save()` | `editor.html`, `editor.js` |
| Play (header) | `#playBtn` | Generate timeline preview | `Preview.generate()` — disabled while busy | `editor.html`, `editor.js` |
| Stop (header) | `#stopBtn` | Stop preview | `Preview.stop()` | `editor.html`, `editor.js` |
| Export Video | `#exportBtn` | Open export dialog | Shows `#exportModal`; disabled during export | `editor.html`, `editor.js` |

### Media library (left panel)

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Project files panel | `.project-panel` | Media library | — | `editor.html` |
| Upload button | `#uploadBtn` | Pick files to upload | Triggers `#mediaFileInput` | `editor.html`, `editor.js` |
| File input | `#mediaFileInput` | Hidden file picker | `MediaLibrary.addFiles()` on change | `editor.html`, `editor.js` |
| Drop zone container | `#project-files-container` | Drag-over target | `staticElementsEvents` drag/drop handlers | `editor.html`, `editor.js` |
| Drop zone | `.drop-zone` | Upload hint | Highlights on drag-over | `editor.html`, `editor.js` |
| Media list | `#media-list` | Uploaded media rows | Populated by `UI.reconciliateMedias()` | `editor.html`, `editor.js` |

#### Dynamic — media item

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Media row | `.file-item[item-hash]` | One media asset | Draggable; click selects; shows properties | `editor.js` |
| Media icon | `.file-icon` | Type icon | `UI.mediaIcon()` | `editor.js` |
| Media name | `.file-name` | Filename | Truncated with `title` tooltip | `editor.js` |
| Media duration | `.file-duration` | Length | `UI.mediaDuration()` | `editor.js` |
| Selected media | `.file-item.selected` | Active selection | Orange border | `editor.css` |

### Preview (center top)

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Preview container | `.preview-container`, `.preview-container--active` | Video preview area | `--active` when video is loaded; hides custom transport bar | `editor.html`, `editor.js` |
| Preview video | `#previewVideo` | Rendered preview player | Native `controls`; shown via `Preview.showVideo()` | `editor.html`, `editor.js` |
| Preview placeholder | `#previewPlaceholder` | Empty state | Shown via `Preview.showPlaceholder()` | `editor.html`, `editor.js` |
| Preview status | `#previewStatus` | Loading / error text | `aria-live="polite"`; auto-hides after ready | `editor.html`, `editor.js` |
| Preview transport bar | `#previewControls`, `.preview-controls` | Generate preview (empty state) | Hidden when video is active; use native video controls | `editor.html`, `editor.js` |

### Timeline (center bottom)

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Timeline container | `.timeline-container` | Tracks + ruler + playhead | Wheel zoom | `editor.html`, `editor.js` |
| Add video track | `#addVideoTrackBtn` | New video track | `Project.addTrack('VIDEO')` | `editor.html`, `editor.js` |
| Add audio track | `#addAudioTrackBtn` | New audio track | `Project.addTrack('AUDIO')` | `editor.html`, `editor.js` |
| Cut clip | `#cutClipBtn` | Split at playhead | `Project.cutSelectedClipAtPlayhead()` — disabled when no clip selected | `editor.html`, `editor.js` |
| Delete clip | `#deleteClipBtn` | Remove selected clip | `Project.requestDeleteClip()` — confirms, disabled when no selection | `editor.html`, `editor.js` |
| Zoom in | `#timelineZoomInBtn` | Increase timeline zoom | `TimelineZoom.zoomIn()` | `editor.html`, `editor.js` |
| Zoom out | `#timelineZoomOutBtn` | Decrease timeline zoom | `TimelineZoom.zoomOut()` | `editor.html`, `editor.js` |
| Fit timeline | `#timelineFitBtn` | Fit all clips | `TimelineZoom.fitToTimeline()` | `editor.html`, `editor.js` |
| Zoom level label | `#zoomLevelLabel` | Current zoom % | Updated by `TimelineZoom` | `editor.html`, `editor.js` |
| Timeline scroll | `.timeline-scroll` | Horizontal scroll bar | Drag `.scroll-handle` | `editor.html`, `editor.js` |
| Ruler | `.timeline-ruler` | Time scale | Click sets playhead | `editor.html`, `editor.js` |
| Ruler marks | `#rulerMarks` | Time tick labels | Built by `TimelineZoom.updateRulerMarks()` | `editor.html`, `editor.js` |
| Tracks container | `#tracks-container` | All track rows | Drop target for media/clips | `editor.html`, `editor.js` |
| Playhead | `#playhead` | Current time indicator | Draggable; red line | `editor.html`, `editor.js` |

#### Dynamic — track row

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Track row | `.track-row[data-track-row]` | One timeline track | — | `editor.js` |
| Track name | `.track-name` | Track label | e.g. "Video Track 1" | `editor.js` |
| Mute track | `.mute-btn[data-track-index]` | Mute toggle | Toggles `track.muted`; `.track-btn--active` when muted | `editor.js` |
| Lock track | `.lock-btn[data-track-index]` | Lock toggle | Toggles `track.locked`; `.track-btn--active` when locked | `editor.js` |
| Remove track | `.remove-track-btn[data-track-index]` | Delete track | Confirms, then `Project.removeTrack()` | `editor.js` |
| Track drop area | `.track-area[data-track-index]` | Clip placement zone | Drag-over highlight (`.active`) | `editor.js` |
| Track line | `.track-line` | Clip container | Drop target within track | `editor.js` |
| Locked track | `.track-area--locked` | Locked visual state | Blocks edits | `editor.css` |

#### Dynamic — clip

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Video clip | `.clip[item-hash]` | Timeline video segment | Draggable; selectable | `editor.js` |
| Audio clip | `.clip.clip--audio[item-hash]` | Timeline audio segment | Same; red styling | `editor.js` |
| Clip name | `.clip-name` | Media name on clip | Truncated in clip bar | `editor.js` |
| Selected clip | `.clip.selected` | Active clip | Orange border | `editor.css` |
| Dragging clip | `.clip.clip--dragging` | In-drag state | Reduced opacity | `editor.js` |
| Shadow clip (drop preview) | `[item-temp-hash]` | Ghost while dragging | Removed on drop/cancel | `editor.js` |
| Time mark | `.time-mark` | Ruler label | Positioned by zoom | `editor.js` |

### Properties (right panel)

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Properties panel | `.properties-panel` | Inspector sidebar | — | `editor.html` |
| Tab: Properties | `.tab.active` (first) | Properties view | Tab switch (visual only) | `editor.html`, `editor.js` |
| Tab: Transitions | `.tab` (second) | Transitions view | Shows `#tabComingSoon` message | `editor.html`, `editor.js` |
| Tab: Effects | `.tab` (third) | Effects view | Shows `#tabComingSoon` message | `editor.html`, `editor.js` |
| Tab coming soon banner | `#tabComingSoon` | Unimplemented tab notice | Visible on Transitions/Effects tabs | `editor.html`, `editor.js` |
| Empty selection hint | `#item-properties-empty` | No selection message | Hidden when item selected | `editor.html`, `editor.js` |
| Item properties | `#item-properties` | Media/clip fields | Built by `UI.showItemProperties()` | `editor.html`, `editor.js` |
| Project name | `#txt-project-name` | Project title | `UI.bindProjectProperties()` | `editor.html`, `editor.js` |
| Screen size | `#cmb-screen-size` | Output resolution | Change → project settings | `editor.html`, `editor.js` |
| Frame rate | `#cmb-frame-rate` | Project FPS | Change → project settings | `editor.html`, `editor.js` |
| Project duration | `#dur-project` | Timeline length | H/M/S/ms spin controls | `editor.html`, `editor.js` |

#### Dynamic — selected item properties

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Media name (read-only) | `#item-properties input[disabled]` | Selected media name | Display only — label "Media Name" | `editor.js` |
| Media duration (read-only) | `#item-properties` duration field | Selected media length | Display only | `editor.js` |
| Clip name | `#item-properties` text input | Clip label | Editable (not yet persisted on blur) | `editor.js` |
| Clip start time | `#item-properties` number input | Start offset (s) | Display/edit | `editor.js` |
| Clip duration | `#item-properties` text input | Clip length | Display | `editor.js` |
| Clip speed | `#item-properties` number input | Playback speed | 0.1–3 | `editor.js` |

### Export modal

| Name | Selector | Purpose | Behavior | Source |
|------|----------|---------|----------|--------|
| Export modal | `#exportModal` | Export dialog overlay | Open/close via buttons or backdrop click | `editor.html`, `editor.js` |
| Close (×) | `.modal-header .close` | Dismiss modal | Hides modal | `editor.html`, `editor.js` |
| Export format | `#exportFormat` | MP4 / WebM | Select before export | `editor.html`, `editor.js` |
| Export quality | `#exportQuality` | Resolution preset | Select before export | `editor.html`, `editor.js` |
| Progress bar | `#progressFill` | Export progress | Width updated during render | `editor.html`, `editor.js` |
| Progress text | `#progressText` | Status message | Updated during export | `editor.html`, `editor.js` |
| Download link | `#exportDownloadLink` | Fetch rendered file | Shown when export completes | `editor.html`, `editor.js` |
| Cancel | `.close-export` | Close without export | Hides modal | `editor.html`, `editor.js` |
| Start export | `#startExportBtn` | Begin render | `Export.start()` → API | `editor.html`, `editor.js` |

---

## Keyboard shortcuts (editor)

| Key | Action | Source |
|-----|--------|--------|
| `Delete` / `Backspace` | Delete selected clip with confirmation (when focus not in input) | `editor.js` |

---

## Nielsen heuristics compliance (audit)

| Heuristic | Status | Notes |
|-----------|--------|-------|
| 1 Visibility of system status | OK | `#editorStatus`, preview status, export progress, busy buttons |
| 2 Match real world | OK | Domain labels; media vs clip naming |
| 3 User control | OK | Modal cancel/close; deselect on timeline/placeholder click |
| 4 Consistency | OK | Shared `btn`, `panel`, tokens, icon sprite |
| 5 Error prevention | OK | Confirm delete track/clip; Cut/Delete disabled without selection |
| 6 Recognition | OK | Labels, tooltips on clips, zoom buttons, track controls |
| 7 Flexibility | OK | Drag-and-drop, keyboard delete, timeline shortcuts |
| 8 Minimalism | OK | Menu items marked disabled until implemented |
| 9 Error recovery | OK | Inline `#editorStatus` — no `alert()` |
| 10 Help | OK | Empty states, `#tabComingSoon`, `title`/`aria-label` on controls |

---

## Selection & identity attributes

| Attribute | Used on | Purpose |
|-----------|---------|---------|
| `item-hash="{hash}"` | `.file-item`, `.clip` | Stable lookup via `UI.getElementByHash` |
| `item-temp-hash` | Shadow clips during drag | Temporary drop preview |
| `data-track-index` | Track controls, `.track-area` | Track lookup |
| `data-track-row` | `.track-row` | Row layout index |
| `data-track-type` | `.track-area` | `VIDEO` or `AUDIO` |
