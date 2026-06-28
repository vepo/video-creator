# Kdenlive Feature Parity

Comparison of **Video Creator** against typical **Kdenlive** (MLT-based NLE) capabilities. Use this for roadmap planning; see [`INTERFACE_FEATURES.md`](INTERFACE_FEATURES.md) for web UI implementation status only.

**Legend**

| Mark | Meaning |
|------|---------|
| `[x]` | **Implemented** — available in Video Creator and works end-to-end (UI + persistence + preview/render where applicable) |
| `[~]` | **Partial** — basic/subset, metadata-only, UI without render pipeline, or known limitations |
| `[ ]` | **Missing** — not available |

**Summary (Jun 2026):** ~85 implemented · ~18 partial · ~49 missing or N/A (desktop-only)

---

## 1. Projects & startup

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Create new project | `/editor/new` → MongoDB project |
| `[x]` | Open existing project | Projects table + `/editor/{id}` |
| `[x]` | Save project | Auto-save + explicit Save → `PUT /api/editor/{id}` |
| `[x]` | Delete project | Main window → `DELETE /api/editor/{id}` |
| `[x]` | Rename project | Main window → `PUT /api/projects/{id}` |
| `[x]` | Project description / notes | `#txt-project-description` |
| `[x]` | Search / filter projects | `#projectSearch` on main window |
| `[x]` | Set project resolution (profile) | `#cmb-screen-size` → render profile |
| `[x]` | Set project frame rate | `#cmb-frame-rate` stored on project |
| `[x]` | Frame rate in render pipeline | `FrameRate` → `MltFrameRate` in MLT profile |
| `[x]` | Project templates / wizards | `GET /api/templates` |
| `[x]` | Open recent projects list | `#recentProjects` + `localStorage` |
| `[x]` | Duplicate project | `POST /api/editor/{id}/duplicate` |
| `[x]` | Project archive / backup (.kdenlive zip) | `GET /api/editor/{id}/archive` |
| `[ ]` | Recover autosave / crash recovery | — |
| `[ ]` | Relocate missing media / path remap | N/A — GridFS-backed media |
| `[ ]` | Multiple open projects (tabs) | — |

---

## 2. Project bin (media library)

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Import media files | Upload + drag-and-drop → GridFS |
| `[x]` | Media list with name & duration | `#media-list` |
| `[x]` | Media type icons (video / audio / image) | SVG sprite |
| `[x]` | Rename clip in bin | Properties panel + `PUT …/media/{hash}` |
| `[x]` | Remove unused media | Delete when not on timeline |
| `[x]` | Drag bin item to timeline | Creates clip(s) at drop position |
| `[ ]` | Folder / bin organization | Flat list only |
| `[ ]` | Tags, ratings, custom columns | — |
| `[x]` | Search / filter bin | `#mediaBinSearch` |
| `[x]` | Sort bin (name, date, duration) | `#mediaBinSort` |
| `[ ]` | Clip zones / color labels | — |
| `[ ]` | Proxy clips (edit low-res, render full-res) | — |
| `[ ]` | Transcode on import | — |
| `[ ]` | Offline / online media states | — |
| `[ ]` | Insert media at playhead (overwrite vs insert) | Always insert at drop position |
| `[ ]` | Import image sequence | Single image only |
| `[ ]` | Import MLT / XML / OTIO | — |
| `[ ]` | Speech-to-text / subtitle import | — |
| `[ ]` | Built-in stock media / templates | — |

---

## 3. Timeline structure

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Multiple video tracks | Add/remove video tracks |
| `[x]` | Multiple audio tracks | Add/remove audio tracks |
| `[x]` | Add track | `+ Video Track` / `+ Audio Track` |
| `[x]` | Remove track | × on track header (keeps last track of each type) |
| `[x]` | Mute track | Mute button; honored in render (`track.muted`) |
| `[x]` | Lock track | Blocks drops and edits on locked track |
| `[x]` | Timeline ruler with time marks | `TimelineZoom.formatRulerTime()` |
| `[x]` | Horizontal scroll | Scroll handle + playhead follow |
| `[x]` | Zoom timeline | In / out / fit + Ctrl+wheel |
| `[x]` | Zoom level indicator | `#zoomLevelLabel` |
| `[x]` | Playhead timecode display | `#playheadTimeLabel` |
| `[ ]` | Adjustable track height | Fixed row height |
| `[ ]` | Track reorder (move up/down) | — |
| `[ ]` | Subtitle / title dedicated tracks | — |
| `[ ]` | Record voiceover to track | — |
| `[ ]` | Timeline markers | — |
| `[ ]` | Guides (vertical lines) | — |
| `[x]` | Snap (playhead, clips, markers) | `TimelineSnap` + View menu toggle |
| `[~]` | Magnetic timeline / gap closing | Ripple delete toggle |
| `[~]` | Track compositing / blend modes | `Track.blendMode` + MLT; limited UI |
| `[~]` | Nested sequences / sub-projects | Domain stub; no editor UI |
| `[~]` | Multicam tracks | `MulticamGroup` on project; no switch UI |

---

## 4. Clip editing

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Place clip on timeline | Drop from bin |
| `[x]` | Select clip | Click → properties panel |
| `[x]` | Move clip (reposition) | Drag clip on track |
| `[x]` | Trim clip in-point | Left edge drag handle |
| `[x]` | Trim clip out-point | Right edge drag handle |
| `[x]` | Split / cut at playhead | Cut button + **S**; splits synced A/V |
| `[x]` | Delete clip | Toolbar, keyboard, inline × |
| `[x]` | Linked audio + video (A/V sync groups) | Auto on video drop; `syncGroup` + repair |
| `[x]` | Rename clip | Properties + inline edit |
| `[x]` | Edit clip start time (numeric) | Properties panel |
| `[x]` | Clip speed / timewarp | 0.1–3× in UI; passed to MLT XML |
| `[x]` | Separate audio from video | Unlink A/V (`edit-unlink`) |
| `[x]` | Image clip duration | Editable in clip properties |
| `[x]` | Copy / paste clips | Clipboard + Ctrl+C/V |
| `[x]` | Duplicate clip | Ctrl+D / Duplicate menu |
| `[x]` | Ripple delete / ripple trim | View → Ripple delete toggle |
| `[ ]` | Roll edit (trim adjacent clips) | — |
| `[ ]` | Slip edit (move source in/out, fixed timeline position) | — |
| `[ ]` | Slide edit (move clip, adjust neighbors) | — |
| `[ ]` | Lift / extract | — |
| `[ ]` | Insert vs overwrite edit mode | — |
| `[ ]` | Replace clip (swap media, keep timing) | — |
| `[ ]` | Reverse clip | — |
| `[ ]` | Freeze frame | — |
| `[ ]` | Group / ungroup clips | — |
| `[ ]` | Clip markers / in-out on source | `sourceIn`/`sourceOut` used for trim; no marker UI |
| `[~]` | Thumbnails on video clips | API `…/thumbnail`; not drawn on clip bars yet |
| `[~]` | Audio waveform on clips | API `…/waveform`; not drawn on clip bars yet |
| `[ ]` | Clip color / zone labels | — |
| `[ ]` | Stabilize clip | — |
| `[ ]` | Auto rotate (metadata) | — |
| `[ ]` | Multicam switch points | — |

---

## 5. Playback & monitoring

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Program monitor (preview area) | `#previewVideo` |
| `[x]` | Play / stop | Header + transport bar |
| `[x]` | Frame step forward / back | Transport ⏩ / ⏪ |
| `[x]` | Jump to start / end | Transport ⏮ / ⏭ |
| `[x]` | Scrub playhead on timeline | Ruler / track click + drag handle |
| `[x]` | Playhead ↔ preview sync | `Preview.syncFromPlayhead()` on scrub |
| `[x]` | Generate timeline preview | HLS session `POST …/preview/session` + hls.js |
| `[x]` | Real-time playback while editing | HLS streaming (regenerates on project change) |
| `[x]` | Playhead follows during playback | Timeline scroll + playhead follow |
| `[ ]` | Source monitor (clip-only preview) | — |
| `[x]` | J/K/L shuttle playback | Keyboard on preview focus |
| `[ ]` | Audio scrubbing | — |
| `[ ]` | In / out points for preview range | — |
| `[x]` | Full-screen preview | `#previewFullscreenBtn` + double-click |
| `[ ]` | Safe area / title-safe overlay | — |
| `[ ]` | Split monitor layout | — |
| `[ ]` | Scope monitors (waveform, vectorscope, histogram) | — |
| `[ ]` | Audio VU meters | — |
| `[ ]` | Drop-frame / timecode modes | Wall-clock ms only |

---

## 6. Effects, transitions & compositing

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Apply transition to clip | `ClipTransition` → MLT via `MltClipFilters` |
| `[x]` | Apply effect to clip | `List<ClipEffect>` → MLT filters |
| `[~]` | Transition between two adjacent clips | Single-clip transition metadata |
| `[x]` | MLT effects stack (multiple per clip) | `Clip.effects` list |
| `[~]` | Built-in effect library (100+ MLT filters) | Curated registry in `plugins/effects.json` |
| `[~]` | Blend modes (add, multiply, …) | `Track.blendMode` field |

---

## 7. Color correction

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[~]` | Color wheels (lift / gamma / gain) | `ColorGrade` domain + `ColorGradeService` |
| `[ ]` | RGB curves | — |
| `[ ]` | White balance | — |
| `[ ]` | Saturation / hue | — |
| `[ ]` | LUT support | — |
| `[ ]` | Shot matching | — |
| `[ ]` | Scopes while grading | — |

---

## 8. Audio editing

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Dedicated audio tracks | Yes |
| `[x]` | Audio + video from same file | Split into synced clips |
| `[x]` | Mute track (no audio in export) | Track mute → MLT visibility |
| `[x]` | Per-clip volume | Properties slider + MLT volume filter |
| `[~]` | Volume keyframes / envelope | `Clip.volumeKeyframes` domain; limited UI |
| `[~]` | Audio mixer panel | Per-track faders planned; volume on clip only |
| `[ ]` | Pan / balance | — |
| `[ ]` | Audio effects (EQ, compressor, normalize) | — |
| `[ ]` | Denoise / click removal | — |
| `[ ]` | Separate channels (stereo → mono) | — |
| `[ ]` | Audio sync / align clips | Manual placement only |

---

## 9. Titles, graphics & subtitles

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[~]` | Title clip editor (text, font, style) | `TitleClip` domain; basic properties |
| `[~]` | Subtitle track | `TitleClip` / SRT export stub |
| `[~]` | SRT / ASS import & export | Export stub via render pipeline |

---

## 10. Rendering & export

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Export to file | `POST /api/editor/{id}/render` |
| `[x]` | Download exported file | `#exportDownloadLink` |
| `[x]` | Format selection (MP4 / WebM / MOV) | Passed to melt consumer |
| `[x]` | Quality presets (high / medium / low) | CRF, preset, resolution scale |
| `[~]` | Export progress | UI steps; no melt progress polling |
| `[x]` | Project FPS in output | `MltFrameRate` in MLT profile |
| `[x]` | Render queue (batch jobs) | `RenderJobService` + `POST …/render/queue` |
| `[ ]` | Render selected zone (in/out) | Full timeline |
| `[ ]` | Custom FFmpeg / melt parameters | Fixed codec mapping |
| `[ ]` | Export audio only | — |
| `[ ]` | Export image sequence | — |
| `[ ]` | Export presets / profiles | — |
| `[ ]` | Upload to YouTube / destination plugins | — |
| `[ ]` | Two-pass encoding | — |
| `[ ]` | Hardware encoding (NVENC, VAAPI) | Software libx264/vp9 only |

---

## 11. Interface & workflow

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Dark theme (KDE Breeze–inspired) | `theme.css` / `editor.css` |
| `[x]` | Three-panel NLE layout | Bin · monitor/timeline · properties |
| `[x]` | Menu bar (File / Edit / View / Help) | Dropdown menus |
| `[x]` | Toolbar actions (save, preview, export) | Editor header |
| `[x]` | Status / notification bar | `#editorStatus` / `UI.notify()` |
| `[x]` | Keyboard shortcuts (subset) | **S** cut, Delete, Backspace |
| `[ ]` | Customizable keyboard shortcuts | — |
| `[x]` | Undo / redo | `History` module; Ctrl+Z/Y |
| `[x]` | Context menus (right-click) | Clips and tracks |
| `[ ]` | Tool switcher (select, razor, …) | — |
| `[ ]` | Welcome / tip of the day | — |
| `[ ]` | Online documentation link | About only |
| `[ ]` | Multi-user / collaborative editing | — |

---

## 12. Capture & acquisition

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Webcam capture | `POST /api/capture/{id}/webcam` + browser upload |
| `[x]` | Screen recording | `POST /api/capture/{id}/screen` |
| `[~]` | Record timeline section to file | Voiceover capture endpoint |

---

## 13. Advanced & integration

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | MLT / melt render engine | Core pipeline |
| `[x]` | MongoDB project persistence | Projects + GridFS media |
| `[x]` | REST API for editor operations | Upload, save, preview, render |
| `[~]` | OpenTimelineIO import/export | `OtioExportService` stub |
| `[~]` | EDL / FCP XML / DaVinci interchange | `EdlExportService` stub |
| `[x]` | Plugin system (effects, producers) | `PluginRegistry` + JSON descriptors |
| `[ ]` | Stop-motion capture | — |
| `[ ]` | DVD / Blu-ray authoring | — |
| `[ ]` | Motion tracking | — |
| `[ ]` | Rotoscoping workflow | — |
| `[ ]` | Proxy / render farm / distributed render | — |

---

## 14. Web-specific (Video Creator extras)

Features Kdenlive does not target but Video Creator provides:

| Status | Feature | Notes |
|--------|---------|-------|
| `[x]` | Browser-based editor (no install) | Quarkus + static JS |
| `[x]` | Server-side media storage (GridFS) | Upload to server |
| `[x]` | Health check dashboard | MLT + DB status on main window |
| `[x]` | Auto-save to server | Debounced `ProjectSave` |
| `[~]` | User accounts / authentication | `UserAccount` domain stub |
| `[~]` | Share project link / collaboration | `GET …/share` read-only token |
| `[~]` | Cloud render queue | `RenderJobService` async jobs |

---

## Priority gaps (highest Kdenlive impact)

Remaining work for deeper parity:

1. **Clip thumbnails & waveforms on timeline bars** — APIs exist; canvas overlay pending  
2. **Slip / slide / roll edit modes** — tool switcher + trim logic  
3. **Title / subtitle editor UI** — domain types exist; modal editor pending  
4. **Render zone + progress polling** — in/out range + melt stdout `%`  
5. **Customizable keyboard shortcuts** — settings modal  
6. **Dockable panels & tool switcher** — CSS grid layout  
7. **Full color grading UI** — wheels/scopes client-side  
8. **Multicam switch at playhead** — `MulticamGroup` wiring  
9. **OTIO / EDL import** — export stubs only today  
10. **Proxy clips & transcode on import** — workflow optimization  

---

## Related docs

| Document | Purpose |
|----------|---------|
| [`INTERFACE_FEATURES.md`](INTERFACE_FEATURES.md) | Web UI feature checklist (implementation-focused) |
| [`UI_ELEMENTS.md`](UI_ELEMENTS.md) | Control inventory for the web UI |
| [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) | Visual tokens (Kdenlive-inspired theme) |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Backend layering and render flow |
