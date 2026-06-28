# Video Creator — User Guide

Last updated: 2026-06-27

Video Creator is a browser-based non-linear video editor. Create **projects**, import **media**, arrange **clips** on a multi-track **timeline**, preview your edit, and **export** a finished video — all from your web browser.

---

## Getting started

1. Open the **main window** (home page).
2. Click **New Project** or open an existing project from the table.
3. In the **editor**, upload media to the **Project Files** panel.
4. Drag media onto **timeline tracks** to create clips.
5. Click **Preview** to watch your timeline.
6. Click **Export Video** to save MP4, WebM, or MOV.

---

## Main window

The home page lists all projects and shows whether the app is ready to preview and export.

| Feature | How to use |
|---------|------------|
| New Project | **New Project** button — opens a fresh editor |
| Open project | Click project name or **Open** in the table |
| Search projects | Type in **Search projects…** to filter the table |
| Rename project | **Rename** on a project row |
| Delete project | **Delete** on a project row (confirmation required) |
| Recent projects | Shown when you return to the main window after editing |
| System status | Indicators for preview/export readiness and storage |

---

## Editor layout

The editor has three columns:

| Region | Purpose |
|--------|---------|
| **Left — Project Files** | Media library, upload, search, and sort |
| **Center — Preview & Timeline** | Video preview, transport controls, multi-track timeline |
| **Right — Properties** | Selected item settings, transitions, effects |

The **header** provides Save, Preview, Stop, Export, and menu bar actions. The **status bar** at the bottom shows save, upload, preview, and export feedback.

Open this guide anytime via **Help → About Video Creator** (opens in a new tab).

---

## File menu

| Action | Description |
|--------|-------------|
| New Project | Start a new empty project in the editor |
| Open Projects… | Return to the main window |
| Save | Save project immediately (also auto-saves) |
| Export Video… | Open export modal |

---

## Edit menu

| Action | Shortcut | Description |
|--------|----------|-------------|
| Undo | Ctrl+Z | Undo last timeline edit |
| Redo | Ctrl+Y | Redo |
| Cut at Playhead | S | Split selected or playhead clip (video + synced audio) |
| Copy | Ctrl+C | Copy selected clip |
| Paste | Ctrl+V | Paste at playhead |
| Duplicate | Ctrl+D | Duplicate selected clip |
| Delete Clip | Delete / Backspace | Remove selected clip |
| Unlink A/V | — | Break sync link between video and audio pair |
| Deselect All | — | Clear selection |

---

## View menu

| Action | Description |
|--------|-------------|
| Zoom In | Increase timeline horizontal zoom |
| Zoom Out | Decrease timeline zoom |
| Fit Timeline | Fit entire timeline duration in view |
| Snap | Toggle clip snap to grid when moving |
| Ripple Delete | When on, deleting a clip closes the gap on its track |

Additional: **Ctrl + mouse wheel** zooms the timeline.

---

## Project Files (media library)

Import and manage source assets before placing them on the timeline.

| Feature | Description |
|---------|-------------|
| Upload button | Opens file picker (video, audio, image) |
| Drag & drop | Drop files on the panel or drop zone |
| Upload progress row | Shows filename and spinning icon while uploading; updates to final media when ready |
| Upload failure | Row shows error state, then removes itself after a few seconds |
| Media icons | Video, audio, image, or unknown type |
| Duration | Shown for each ready media item |
| Select media | Click to show properties in the right panel |
| Drag to timeline | Drag a media row onto a compatible track |
| Search | **Search media…** filters the list by name |
| Sort | By name, duration, or type |
| Rename | Edit name in Properties when media is selected |
| Remove | Delete media from the project |

---

## Timeline

Arrange clips on video and audio tracks with professional NLE-style controls.

### Tracks

| Feature | Description |
|---------|-------------|
| Video / audio tracks | Default tracks; add more with **+ Video Track** / **+ Audio Track** |
| Mute | Toggle track mute (audio silenced on export) |
| Lock | Prevent edits on locked tracks |
| Remove track | Delete empty or unused tracks (with confirmation) |

### Clips

| Feature | Description |
|---------|-------------|
| Add clip | Drag media from library onto a track |
| Move clip | Drag clip body to new time or track (creates gaps; synced A/V moves together) |
| Trim | Drag left/right **trim handles** on clip edges |
| Select | Click clip to select and edit properties |
| Cut at playhead | Place playhead inside clip, press **S** or **Cut** |
| Delete | Select clip and press Delete or use **Delete** toolbar button |
| Context menu | Right-click clip for cut, copy, paste, duplicate, delete, unlink, properties |
| Clip actions | Hover buttons for properties (…) and delete (×) |
| Shadow preview | Semi-transparent preview while dragging |

### Playhead & ruler

| Feature | Description |
|---------|-------------|
| Playhead line | Red line across **track areas only** (not over track headers) |
| Ruler marker | Matching tick in the time ruler |
| Scrub | Click or drag on ruler or empty track area |
| Timecode | **Position** label in timeline toolbar |
| Scroll | Horizontal scroll bar below ruler; drag handle to pan |

### Zoom

| Feature | Description |
|---------|-------------|
| Zoom in / out | Toolbar buttons or View menu |
| Fit timeline | Show full project duration |
| Zoom label | **Zoom: N%** in timeline header |
| Ctrl + wheel | Zoom while pointer is over timeline |

---

## Preview

Watch your timeline without exporting. Preview quality is optimized for speed so you can iterate quickly.

| Feature | Description |
|---------|-------------|
| Start preview | **Preview** in header or preview transport **Play** |
| Progress | Status bar shows **Rendering preview: N% — ETA** while the preview is prepared |
| Stop | **Stop** clears preview and resets playhead |
| Transport | Previous frame, rewind, play/pause, forward, next frame |
| Fullscreen | **Full** button or double-click preview area |
| Shuttle keys | **J** rewind / slow, **K** pause, **L** forward / faster (when video loaded) |
| Playhead sync | Playhead follows playback; scrubbing seeks preview |
| Requirements | The project must contain at least one clip; preview must be available (see System status on the main window) |

---

## Properties panel

### Tabs

| Tab | Purpose |
|-----|---------|
| Properties | Media, clip, or project fields |
| Transitions | Apply transition type to selected clip |
| Effects | Apply effect to selected clip |

### Project settings (no selection)

| Field | Description |
|-------|-------------|
| Name | Project title |
| Description | Optional notes |
| Screen size | Output resolution preset |
| Frame rate | Timeline frame rate |
| Duration | Timeline length setting |

### Media properties

| Field | Description |
|-------|-------------|
| Name | Editable display name |
| Duration | Read-only |

### Clip properties

| Field | Description |
|-------|-------------|
| Name | Clip label on timeline |
| Start | Timeline start time (ms) |
| Duration | Clip length on timeline |
| Speed | Playback speed multiplier |

Changes auto-save after editing (debounced).

---

## Export

| Feature | Description |
|---------|-------------|
| Open modal | **Export Video** header button or File menu |
| Format | MP4, WebM, or MOV |
| Quality | High, Medium, or Low |
| Progress | Modal progress bar: save → render → complete |
| Download | Link appears when render finishes |
| Cancel | Close modal (export may continue in the background) |

---

## Keyboard shortcuts

| Key | Action |
|-----|--------|
| Ctrl+Z | Undo |
| Ctrl+Y | Redo |
| Ctrl+C | Copy clip |
| Ctrl+V | Paste clip |
| Ctrl+D | Duplicate clip |
| S | Cut at playhead |
| Delete / Backspace | Delete selected clip |
| J | Shuttle backward |
| K | Pause shuttle |
| L | Shuttle forward |

Shortcuts are ignored while typing in input fields.

---

## Status bar

The footer **status bar** shows contextual messages:

| Message type | Examples |
|--------------|----------|
| Info | Uploading…, Rendering preview: 45% — ETA 1:30 |
| Success | Added clip.mp4 to project., Preview ready., Save complete |
| Error | Upload failed, Preview failed, Export failed |

Info and success messages auto-clear after a few seconds; errors stay until the next action.

---

## Tips & troubleshooting

| Issue | What to try |
|-------|-------------|
| Preview unavailable | Check **System status** on the main window; add at least one clip to the timeline |
| Preview slow | First preview takes longer to prepare; the status bar shows progress and estimated time |
| Cut does nothing | Move playhead **inside** a clip (not on edges) |
| Cannot drop on track | Match media type (video/image → video track, audio → audio track) |
| Track locked | Unlock track before moving or trimming clips |
| Export fails | Verify all clips use valid media; try again after saving the project |
