# Architecture — Video Creator

## Overview

Video Creator is a **Quarkus** application that provides a browser-based video editor. Users create **Projects**, upload **Media** to a project library, arrange content on a timeline, and **Render** or **Preview** output. Video processing is delegated to the **MLT Framework** through the `melt` CLI; media metadata is extracted with `ffmpeg`, `soxi`, and `file`.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (HTML/JS/CSS)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP
┌────────────────────────────▼────────────────────────────────────┐
│  Delivery Layer                                                 │
│  VideoEditorController (Qute pages, multipart upload)           │
│  VideoEditorResource   (JSON REST, health)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│  Application Layer (service/)                                   │
│  MediaService          — store/retrieve media, probe duration   │
│  VideoProcessingService — orchestrate melt renders              │
│  MLTXmlGenerator       — build MLT XML from domain/DTO input    │
└──────────────┬─────────────────────────────┬────────────────────┘
               │                             │
┌──────────────▼──────────────┐   ┌──────────▼────────────────────┐
│  Domain (project/)          │   │  Infrastructure               │
│  Project, Media, Clip       │   │  MongoDB (Projects repo)      │
│  ScreenSize, FrameRate      │   │  GridFS (MediaService)        │
│  MediaType                  │   │  melt / ffmpeg / soxi (OS)    │
└─────────────────────────────┘   │  infra/ (serializers, mappers)│
                                  └───────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21, Quarkus 3.37 |
| DI | CDI (Quarkus Arc) |
| HTTP | JAX-RS (RESTEasy Reactive) |
| Templates | Qute |
| Persistence | MongoDB + GridFS |
| Video engine | MLT (`melt`) |
| Config | `application.properties`, `@ConfigMapping` |

## Bounded Context: Video Editing

The core bounded context is **video project editing**:

- **Aggregate root**: `Project` — owns media references and clips, defines output settings (`ScreenSize`, `FrameRate`, `duration`).
- **Entities**: `Media` (library item with GridFS id, hash, mime type, duration), `Clip` (timeline placement; evolving).
- **Value objects**: `ScreenSize`, `FrameRate`, `MediaType`.

### Current State vs Target

The codebase is mid-migration toward clean DDD layering:

| Area | Current | Target |
|------|---------|--------|
| Project persistence | `Projects` in `project/` talks to Mongo directly | `ProjectRepository` port in domain; Mongo adapter in `infra/` |
| Timeline editing | `model.TimelineProject` (API DTO with behavior) | Consolidate into `project.Project` or explicit `Timeline` value object |
| Media upload | Controller mutates `project.getMedias()` then saves | `Project.addMedia(media)` domain method; application service coordinates |
| MLT execution | `VideoProcessingService` runs `ProcessBuilder` | Acceptable in application/infra boundary; extract `MeltRenderer` port if testing needs grow |

New work should move **toward** the target column, not add coupling in the current direction.

## Request Flows

### 1. Open Editor

```
GET /editor/{projectId}
  → Projects.find(id) or Projects.newProject()
  → Qute renders editor.html with Project JSON
```

### 2. Upload Media

```
POST /api/editor/{projectId}/media  (multipart)
  → Projects.find(projectId)
  → MediaService.store(tempFile)     → GridFS + Media entity
  → project.medias.add(media); Projects.update(project)
```

### 3. Render Timeline

```
POST /api/editor/{projectId}/render
  → TimelineAssembler.assemble(Project)
  → VideoProcessingService.processTimelineProject()
      → MLTXmlGenerator.generateTimelineMLTXml()
      → melt -consumer avformat:...
  → output file in app.output-dir
```

### 4. Health Check

```
GET /api/video/health
  → VideoProcessingService.isMeltAvailable()
```

## Data Storage

| Store | Collection/Bucket | Content |
|-------|-------------------|---------|
| MongoDB | `project` | `Project` documents (POJO codec) |
| GridFS | `media` | Binary uploads with metadata (hash, mime-type, uploaded-at) |
| Filesystem | `uploads/`, `output/`, `temp/` | Configured via `app.*-dir` |

`Project` embeds `Media` references (ObjectId + metadata), not file bytes.

## Configuration

`AppConfig` maps the `app` prefix:

| Property | Purpose |
|----------|---------|
| `app.upload-dir` | Upload staging |
| `app.output-dir` | Rendered videos |
| `app.temp-dir` | Temporary files |
| `app.melt-command` | Path to `melt` binary |
| `app.default-*-codec`, `app.default-crf` | Encoding defaults |

Mongo database name: `mongodb.database` in `application.properties`.

## Frontend

| Path | Role |
|------|------|
| `templates/index.html` | Project list, MLT status |
| `templates/editor.html` | Timeline editor shell |
| `META-INF/resources/javascript/` | Client-side editor logic |
| `META-INF/resources/css/` | Styles |

The editor persists `Project` JSON via `PUT /api/editor/{id}`; preview uses HLS sessions (`POST …/preview/session`); export uses `POST …/render` or the render queue.

## Layer Rules

1. **Domain** must not depend on Quarkus, JAX-RS, MongoDB, or OS process APIs.
2. **Controllers** translate HTTP ↔ application calls; no `ProcessBuilder`, no GridFS.
3. **Services** coordinate use cases; delegate persistence to repositories and rendering to MLT adapters.
4. **Infrastructure** implements technical details (serializers, exception mappers, future repository adapters).

## Extension Points

When adding features, prefer these seams:

- **New domain behavior** → methods on `Project`, `Clip`, or new value objects in `project/`
- **New persistence** → repository interface + `infra` implementation
- **New render pipeline** → `MLTXmlGenerator` + `VideoProcessingService` (or extracted `Renderer` port)
- **New API** → thin resource/controller + application service method

## Testing Strategy

| Layer | Test type |
|-------|-----------|
| Domain (`project/`) | Pure JUnit 5 unit tests, no Quarkus |
| Services | Unit tests with mocked ports; `@QuarkusTest` for integration |
| Controllers | `@QuarkusTest` + REST Assured |
| MLT integration | Optional manual/CI job with `melt` installed |

Tests live in `src/test/java`, mirroring production packages.
