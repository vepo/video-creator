# Agent Guide — Video Creator

Quarkus video-editing service with a web GUI. Users manage **Projects**, upload **Media**, arrange **Clips** on a timeline, and render output via MLT's `melt` command.

## Quick Reference

| Item | Value |
|------|-------|
| Language | Java 21 |
| Framework | Quarkus 3.37 |
| Build | Maven (`./mvnw` or `mvn`) |
| Database | MongoDB (projects + GridFS media) |
| External tools | `melt`, `ffmpeg`, `soxi`, `file` |

## Commands

```bash
# Dev mode (hot reload)
./mvnw quarkus:dev

# Compile
./mvnw compile

# Run tests (write tests before production code — see TDD rule)
./mvnw test

# Package
./mvnw package
```

Prerequisites: Java 21+, Maven, MongoDB, MLT (`melt`), Sox, FFmpeg. See [README.md](README.md).

## Package Layout

```
src/main/java/dev/vepo/youtube/creator/
├── project/          # Domain: Project, Media, Clip, value objects
├── service/          # Application services (video processing, media storage)
├── model/            # API/DTO types for timeline JSON (migrate toward project/)
├── infra/            # Mongo serializers, JAX-RS exception mappers
├── VideoEditorController.java   # Web UI + multipart/form endpoints
├── VideoEditorResource.java     # JSON REST API
└── AppConfig.java                # Typed configuration (@ConfigMapping)
```

Frontend: `src/main/resources/templates/` (Qute HTML) and `META-INF/resources/` (CSS/JS). UI inventory: [`docs/UI_ELEMENTS.md`](docs/UI_ELEMENTS.md).

## Visual Identity

Kdenlive-inspired dark theme (KDE Breeze Dark). See [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md).

- Shared tokens: `META-INF/resources/css/theme.css`
- Main window: projects list, New Project, system status
- Editor: three-column NLE layout
- Icons: SVG sprite at `/icons/icons.svg` — no emoji in UI chrome

## Architecture

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before structural changes.

**Dependency direction (inward):**

```
Controllers / Resources  →  Application Services  →  Domain  ←  Infrastructure
```

- **Domain** (`project/`): entities, value objects, domain behavior. No Quarkus, MongoDB, or `melt` imports.
- **Application services** (`service/`): orchestrate use cases; depend on domain abstractions.
- **Infrastructure** (`infra/`, Mongo in `Projects`/`MediaService`): persistence, external process execution, serializers.
- **Delivery** (controllers/resources): HTTP, templates, request/response mapping only.

## Domain Language

Use ubiquitous language consistently in code, tests, and docs:

| Term | Meaning |
|------|---------|
| **Project** | A video editing workspace with timeline settings, media library, and clips |
| **Media** | Uploaded asset (video, audio, image) stored in GridFS |
| **Clip** | A segment of media placed on the project timeline |
| **Render** | Produce a final video file via `melt` |
| **Preview** | Short, lower-quality render for timeline feedback |
| **Timeline** | Ordered tracks of clips with in/out points |

Prefer `project/` types over `model/` DTOs for new domain work. Rename or consolidate duplicates (e.g. `TimelineProject` vs `Project`) rather than adding a third concept.

## Development Workflow

1. **Red** — Write a failing test that expresses the desired behavior.
2. **Green** — Minimal code to pass.
3. **Refactor** — Improve design; keep tests green.

Place tests in `src/test/java` mirroring the production package structure. Use `@QuarkusTest` for integration tests; plain JUnit 5 for pure domain logic.

## Cursor Rules

Project rules live in `.cursor/rules/`:

| Rule | Scope |
|------|-------|
| `java-quality.mdc` | Java style and quality |
| `javascript-standards.mdc` | Browser editor JS (vanilla, namespaces) |
| `html-standards.mdc` | Qute templates and page markup |
| `nielsen-heuristics.mdc` | Nielsen usability heuristics for UI |
| `ui-elements-catalog.mdc` | UI inventory — read/update `docs/UI_ELEMENTS.md` |
| `tdd-development.mdc` | Test-first workflow |
| `ddd-domain-language.mdc` | Domain-driven naming and boundaries |
| `architecture-correctness.mdc` | Layering and dependency rules |
| `tell-dont-ask.mdc` | Object-oriented design |

## Conventions

- Inject dependencies via CDI (`@Inject`); prefer `@ApplicationScoped` for services.
- Configuration via `AppConfig` (`@ConfigMapping(prefix = "app")`), not scattered `@ConfigProperty`.
- Return typed response records/DTOs from REST endpoints; avoid hand-built JSON strings.
- Log with SLF4J; never `System.out` / `System.err` in production code.
- Keep changes focused; match existing naming and package placement.

## What Not to Do

- Do not put business logic in controllers.
- Do not import infrastructure (Mongo, `ProcessBuilder`) into domain classes.
- Do not add features without tests.
- Do not commit secrets or local paths from `application.properties` overrides.
- Do not expand `model/` with new domain concepts — extend `project/` instead.
