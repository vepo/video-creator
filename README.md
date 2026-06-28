# Video Creator

Browser-based non-linear video editor built with **Quarkus**. Create **projects**, import **media**, arrange **clips** on a multi-track **timeline**, preview your edit, and **export** finished video — all from a Kdenlive-inspired web UI.

Rendering and preview use the [MLT Framework](https://www.mltframework.org/) (`melt`). Projects and uploaded media are stored in **MongoDB** (GridFS for binary assets).

**In-app user guide:** run the app and open [`/docs`](http://localhost:8080/docs), or use **Help → About Video Creator** in the editor.

---

## What you can do

| Area | Capabilities |
|------|----------------|
| **Projects** | Create, open, rename, search, and delete projects from the main window |
| **Media library** | Upload video, audio, and images (picker or drag-and-drop); search, sort, rename, remove |
| **Timeline** | Multi-track video and audio editing — move, trim, cut, copy/paste, duplicate, ripple delete, snap, zoom |
| **Preview** | Stream a timeline preview with transport controls, scrubbing, and shuttle keys (J/K/L) |
| **Export** | Render MP4, WebM, or MOV at High / Medium / Low quality |
| **Editing** | Undo/redo, linked A/V sync, track mute/lock, transitions and effects (UI; render wiring in progress) |

For a full UI feature checklist see [`docs/INTERFACE_FEATURES.md`](docs/INTERFACE_FEATURES.md). For Kdenlive parity roadmap see [`docs/KDENLIVE_FEATURES.md`](docs/KDENLIVE_FEATURES.md).

---

## Technology stack

| Layer | Choice |
|-------|--------|
| Runtime | Java 21, Quarkus 3.37 |
| UI | Qute templates, vanilla JavaScript, CSS (KDE Breeze Dark theme) |
| API | JAX-RS (RESTEasy Reactive), WebSockets (preview progress) |
| Persistence | MongoDB + GridFS |
| Video engine | MLT (`melt`), FFmpeg, Sox (`soxi`) for media probing |

Architecture, layering, and request flows: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Prerequisites

| Requirement | Purpose |
|-------------|---------|
| **Java 21+** | Application runtime |
| **Maven 3.9+** | Build and dev mode |
| **MongoDB** | Project and media storage (Quarkus Dev Services can start a container in dev/test when none is configured) |
| **MLT** (`melt`) | Timeline preview and export |
| **FFmpeg** | Media analysis |
| **Sox** (`soxi`) | Audio duration probing |

---

## Quick start

### 1. Install system tools

**Ubuntu / Debian:**

```bash
sudo apt install melt ffmpeg sox libsox-fmt-mp3
```

Ensure `melt` is on your `PATH` (default config expects `/usr/bin/melt`).

**MongoDB:** install locally, or rely on Quarkus Dev Services during development (Docker required for the auto-started container).

### 2. Run in dev mode

```bash
git clone <repository-url>
cd video-creator
mvn quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080) for the main window. Click **New Project** to open the editor.

Dev mode enables hot reload for Java and static assets.

### 3. Verify system status

On the main window, the **System status** panel shows whether preview/export (MLT) and storage (database) are ready. Fix any red indicators before previewing or exporting.

---

## Build and test

```bash
# Compile
mvn compile

# Unit and integration tests
mvn test

# Full verify (tests, Spotless, packaging)
mvn verify

# Production JAR
mvn package
# Runnable: java -jar target/quarkus-app/quarkus-run.jar
```

Web UI tests use Selenium with headless Chrome. CI sets `GITHUB_ACTIONS=true` for headless runs (see [`.github/workflows/build.yml`](.github/workflows/build.yml)).

---

## Configuration

Key settings in [`src/main/resources/application.properties`](src/main/resources/application.properties):

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | `8080` | HTTP port |
| `mongodb.database` | `video-creator` | MongoDB database name |
| `app.melt-command` | `/usr/bin/melt` | Path to `melt` binary |
| `app.upload-dir` | `uploads` | Temporary upload directory |
| `app.output-dir` | `output` | Rendered preview/export files |
| `app.default-crf` | `23` | Default video quality (lower = higher quality) |

Override locally via `application.properties` in the project root or `%dev` profile without committing secrets or machine-specific paths.

---

## Project layout

```
src/main/java/dev/vepo/youtube/creator/
├── project/          # Domain: Project, Media, Clip, value objects
├── service/          # Application services (media, video processing, timeline)
├── infra/            # Mongo serializers, exception mappers
├── VideoEditorController.java   # Web UI + multipart endpoints
├── VideoEditorResource.java     # JSON REST API
└── DocumentationController.java # /docs user guide

src/main/resources/
├── templates/        # Qute HTML (main window, editor, docs)
├── documentation/    # USER_GUIDE.md → rendered at /docs
└── META-INF/resources/   # CSS, JavaScript, icons

docs/                 # Architecture, design system, feature catalogs
```

Agent and contribution conventions: [`AGENTS.md`](AGENTS.md).

---

## Contributing

We welcome bug reports, documentation improvements, and pull requests. See
[`CONTRIBUTING.md`](CONTRIBUTING.md) for setup, workflow, and PR guidelines.
Please follow our [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). To report security
issues privately, see [`SECURITY.md`](SECURITY.md).

---

## Documentation

| Document | Audience | Contents |
|----------|----------|----------|
| [`/docs`](http://localhost:8080/docs) (in-app) | Users | Workflows, menus, shortcuts, troubleshooting |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Developers | Layers, stack, request flows |
| [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) | UI contributors | Theme tokens, layout, Kdenlive-inspired patterns |
| [`docs/INTERFACE_FEATURES.md`](docs/INTERFACE_FEATURES.md) | Contributors | Web UI feature checklist |
| [`docs/KDENLIVE_FEATURES.md`](docs/KDENLIVE_FEATURES.md) | Product / dev | Kdenlive parity roadmap |
| [`docs/UI_ELEMENTS.md`](docs/UI_ELEMENTS.md) | UI contributors | Element inventory and selectors |

---

## License

This project is licensed under the [GNU General Public License v2](LICENSE).
