# Kdenlive Feature Parity

Comparison of **Video Creator** against typical **Kdenlive** (MLT-based NLE) capabilities. Use this for roadmap planning; see [`INTERFACE_FEATURES.md`](INTERFACE_FEATURES.md) for web UI implementation status only.

**Legend**

| Mark | Meaning |
|------|---------|
| `[x]` | **Implemented** — available in Video Creator and works end-to-end (UI + persistence + preview/render where applicable) |
| `[~]` | **Partial** — basic/subset, metadata-only, UI without render pipeline, or known limitations |
| `[ ]` | **Missing** — not available |

**Summary (Mar 2026):** ~45 implemented · ~12 partial · ~95 missing

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
| `[~]` | Frame rate in render pipeline | `FrameRate` persisted; MLT profile still uses fixed 60 fps in `MLTXmlGenerator` |
| `[ ]` | Project templates / wizards | — |
| `[ ]` | Open recent projects list | — |
| `[ ]` | Duplicate project | — |
| `[ ]` | Project archive / backup (.kdenlive zip) | — |
| `[ ]` | Recover autosave / crash recovery | — |
| `[ ]` | Relocate missing media / path remap | — |
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
| `[ ]` | Search / filter bin | — |
| `[ ]` | Sort bin (name, date, duration) | — |
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
| `[ ]` | Snap (playhead, clips, markers) | — |
| `[ ]` | Magnetic timeline / gap closing | — |
| `[ ]` | Track compositing / blend modes | Tractor stacks tracks; no blend UI |
| `[ ]` | Nested sequences / sub-projects | — |
| `[ ]` | Multicam tracks | — |

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
| `[~]` | Separate audio from video | Created as two clips on import; no “unlink” toggle |
| `[~]` | Image clip duration | Image on video track; duration from project/clip |
| `[ ]` | Copy / paste clips | — |
| `[ ]` | Duplicate clip | — |
| `[ ]` | Ripple delete / ripple trim | — |
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
| `[ ]` | Thumbnails on video clips | Text label only |
| `[ ]` | Audio waveform on clips | — |
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
| `[x]` | Generate timeline preview | `POST /api/editor/{id}/preview` (full low-res render) |
| `[~]` | Real-time playback while editing | Preview is pre-rendered file, not live MLT |
| `[~]` | Playhead follows during playback | Timeline scroll + playhead follow |
| `[ ]` | Source monitor (clip-only preview) | — |
| `[ ]` | J/K/L shuttle playback | — |
| `[ ]` | Audio scrubbing | — |
| `[ ]` | In / out points for preview range | — |
| `[ ]` | Full-screen preview | — |
| `[ ]` | Safe area / title-safe overlay | — |
| `[ ]` | Split monitor layout | — |
| `[ ]` | Scope monitors (waveform, vectorscope, histogram) | — |
| `[ ]` | Audio VU meters | — |
| `[ ]` | Drop-frame / timecode modes | Wall-clock ms only |

---

## 6. Effects, transitions & compositing

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[~]` | Apply transition to clip | UI stores `Clip.transition`; **not in MLT render** |
| `[~]` | Apply effect to clip | UI stores `Clip.effect`; **not in MLT render** |
| `[ ]` | Transition between two adjacent clips | — |
| `[ ]` | Transition duration & alignment | — |
| `[ ]` | MLT effects stack (multiple per clip) | — |
| `[ ]` | Effect keyframes | — |
| `[ ]` | Effect presets / favorites | — |
| `[ ]` | Built-in effect library (100+ MLT filters) | — |
| `[ ]` | Custom effect parameters UI | — |
| `[ ]` | Alpha / chroma key | — |
| `[ ]` | Transform (position, scale, rotation) | — |
| `[ ]` | Crop & pan / zoom (Ken Burns) | — |
| `[ ]` | Masking / rotoscoping | — |
| `[ ]` | Compositing (picture-in-picture) | — |
| `[ ]` | Blend modes (add, multiply, …) | — |
| `[ ]` | Auto fade in/out audio | — |

---

## 7. Color correction

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[ ]` | Color wheels (lift / gamma / gain) | — |
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
| `[~]` | Per-clip volume | Not exposed |
| `[ ]` | Volume keyframes / envelope | — |
| `[ ]` | Audio mixer panel | — |
| `[ ]` | Pan / balance | — |
| `[ ]` | Audio effects (EQ, compressor, normalize) | — |
| `[ ]` | Denoise / click removal | — |
| `[ ]` | Separate channels (stereo → mono) | — |
| `[ ]` | Audio sync / align clips | Manual placement only |

---

## 9. Titles, graphics & subtitles

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[ ]` | Title clip editor (text, font, style) | — |
| `[ ]` | Animated titles | — |
| `[ ]` | Image / SVG overlays | Image media on timeline only |
| `[ ]` | Subtitle track | — |
| `[ ]` | SRT / ASS import & export | — |
| `[ ]` | Speech-to-text subtitles | — |
| `[ ]` | Lower thirds templates | — |

---

## 10. Rendering & export

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | Export to file | `POST /api/editor/{id}/render` |
| `[x]` | Download exported file | `#exportDownloadLink` |
| `[x]` | Format selection (MP4 / WebM / MOV) | Passed to melt consumer |
| `[x]` | Quality presets (high / medium / low) | CRF, preset, resolution scale |
| `[~]` | Export progress | UI steps; no melt progress polling |
| `[~]` | Project FPS in output | Profile width/height; fps hardcoded in MLT |
| `[ ]` | Render queue (batch jobs) | Single job at a time |
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
| `[ ]` | Undo / redo | — |
| `[ ]` | Edit history panel | — |
| `[ ]` | Configurable UI layouts | Fixed layout |
| `[ ]` | Dock / undock panels | — |
| `[ ]` | Context menus (right-click) | — |
| `[ ]` | Tool switcher (select, razor, …) | — |
| `[ ]` | Welcome / tip of the day | — |
| `[ ]` | Online documentation link | About only |
| `[ ]` | Multi-user / collaborative editing | — |

---

## 12. Capture & acquisition

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[ ]` | Webcam capture | — |
| `[ ]` | Screen recording | — |
| `[ ]` | FireWire / DeckLink capture | — |
| `[ ]` | Capture monitor | — |
| `[ ]` | Record timeline section to file | — |

---

## 13. Advanced & integration

| Status | Feature | Video Creator notes |
|--------|---------|---------------------|
| `[x]` | MLT / melt render engine | Core pipeline |
| `[x]` | MongoDB project persistence | Projects + GridFS media |
| `[x]` | REST API for editor operations | Upload, save, preview, render |
| `[ ]` | OpenTimelineIO import/export | — |
| `[ ]` | EDL / FCP XML / DaVinci interchange | — |
| `[ ]` | Scripting (Python / Node) | — |
| `[ ]` | Plugin system (effects, producers) | — |
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
| `[ ]` | User accounts / authentication | — |
| `[ ]` | Share project link / collaboration | — |
| `[ ]` | Cloud render queue | — |

---

## Priority gaps (highest Kdenlive impact)

Recommended next steps to move toward credible NLE parity:

1. **Undo / redo** — table stakes for any editor  
2. **Real-time or incremental preview** — avoid full re-render on every change  
3. **Transitions & effects in MLT pipeline** — wire `Clip.transition` / `Clip.effect` in `TimelineAssembler` + `MLTXmlGenerator`  
4. **Project frame rate in MLT profile** — use `FrameRate` instead of hardcoded 60 fps  
5. **Snap + ripple edit** — standard timeline editing ergonomics  
6. **Copy/paste clips** — fast iteration  
7. **Audio volume + keyframes** — basic mix control  
8. **Clip thumbnails & waveforms** — visual timeline feedback  
9. **Title / subtitle tracks** — common YouTube workflow  
10. **Render zone + queue** — export only what you need, batch exports  

---

## Related docs

| Document | Purpose |
|----------|---------|
| [`INTERFACE_FEATURES.md`](INTERFACE_FEATURES.md) | Web UI feature checklist (implementation-focused) |
| [`UI_ELEMENTS.md`](UI_ELEMENTS.md) | Control inventory for the web UI |
| [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) | Visual tokens (Kdenlive-inspired theme) |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Backend layering and render flow |
