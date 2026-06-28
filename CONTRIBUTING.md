# Contributing to Video Creator

Thank you for your interest in contributing. Video Creator is a browser-based
non-linear video editor built with Quarkus, MLT, and MongoDB. This guide
covers how to set up a development environment, follow project conventions, and
submit changes.

By participating, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to contribute

* **Report bugs** — Open an issue with steps to reproduce, expected behavior,
  and what actually happened.
* **Suggest features** — Describe the user workflow and why it matters; check
  [`docs/KDENLIVE_FEATURES.md`](docs/KDENLIVE_FEATURES.md) for roadmap context.
* **Improve documentation** — User guide, README, architecture notes, or UI
  catalogs.
* **Submit code** — Bug fixes, tests, UI polish, or new features with tests.

## Before you start

1. Read [`README.md`](README.md) for prerequisites and quick start.
2. Skim [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) before structural changes.
3. For agent-assisted or automated workflows, see [`AGENTS.md`](AGENTS.md).

### Prerequisites

| Requirement | Purpose |
|-------------|---------|
| Java 21+ | Application runtime |
| Maven 3.9+ | Build and tests |
| MongoDB | Project and media storage (Dev Services can start a container in dev/test when Docker is available) |
| MLT (`melt`) | Preview and export |
| FFmpeg | Media analysis |
| Sox (`soxi`) | Audio duration probing |
| Docker | Optional; used by Quarkus Dev Services for MongoDB in dev/test |
| Chrome | Required for web UI tests (headless in CI) |

### Local development

```bash
git clone https://github.com/vepo/video-creator.git
cd video-creator
mvn quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080). Dev mode reloads Java and
static assets on change.

## Development workflow

We follow **test-driven development**:

1. **Red** — Write a failing test that expresses the desired behavior.
2. **Green** — Implement the smallest change that passes.
3. **Refactor** — Improve design while keeping tests green.

Do not add user-visible features without tests.

### Architecture

Keep dependency direction inward:

```
Controllers / Resources → Application Services → Domain ← Infrastructure
```

| Layer | Package | Rules |
|-------|---------|-------|
| Domain | `project/` | Entities and value objects; no Quarkus, MongoDB, or `melt` imports |
| Application | `service/` | Orchestrate use cases |
| Infrastructure | `infra/`, persistence adapters | MongoDB, serializers, external processes |
| Delivery | controllers, resources | HTTP, Qute templates, request mapping only |

* Do not put business logic in controllers.
* Prefer `project/` types over `model/` for new domain work.
* Use ubiquitous language: **Project**, **Media**, **Clip**, **Timeline**,
  **Preview**, **Render**.

### Code style

* Inject dependencies with CDI (`@Inject`); prefer `@ApplicationScoped` for services.
* Configure via `AppConfig` (`@ConfigMapping`), not scattered `@ConfigProperty`.
* Log with SLF4J; avoid `System.out` / `System.err` in production code.
* Match existing naming, package placement, and formatting in touched files.
* Do not commit secrets or machine-specific paths from `application.properties`.

Frontend conventions live in `.cursor/rules/` (`javascript-standards.mdc`,
`html-standards.mdc`, `visual-identity.mdc`). UI work should follow
[`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) (Kdenlive-inspired dark theme,
`var(--vc-*)` tokens, no emoji in UI chrome).

## Running tests

```bash
# All tests
mvn test

# By layer (JUnit tags)
mvn test -Ptest-unit
mvn test -Ptest-quarkus
mvn test -Ptest-web

# Full verify (recommended before opening a PR)
mvn verify
```

Web tests use Selenium with headless Chrome. CI sets `GITHUB_ACTIONS=true`.

Place tests under `src/test/java`, mirroring the production package structure:

| Layer | Annotation |
|-------|------------|
| Unit | `@UnitTest` |
| Integration | `@QuarkusIntegrationTest` |
| Web UI | `@WebPlatformTest`, `@WebEditorTest` |

## Documentation updates

Update docs in the **same change** when behavior changes:

| Change type | Update |
|-------------|--------|
| User-visible feature | [`src/main/resources/documentation/USER_GUIDE.md`](src/main/resources/documentation/USER_GUIDE.md) — bump `Last updated:` |
| UI feature status | [`docs/INTERFACE_FEATURES.md`](docs/INTERFACE_FEATURES.md) |
| New or renamed UI element | [`docs/UI_ELEMENTS.md`](docs/UI_ELEMENTS.md) |
| Architecture / layering | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) |

The in-app guide at `/docs` is rendered from `USER_GUIDE.md`; verify in dev mode
after editing.

## Submitting a pull request

1. Fork the repository and create a branch from `main`.
2. Make focused commits; one logical change per PR when possible.
3. Run `mvn verify` locally.
4. Open a pull request against `main` with:
   * A clear summary of **what** changed and **why**
   * Links to related issues, if any
   * Notes on testing performed
   * Screenshots or screen recordings for UI changes
5. Ensure CI passes (build, tests, SonarQube analysis).

Write commit messages and PR descriptions in complete sentences. Prefer the
imperative mood for commit subjects (e.g. "Add clip speed property to export
pipeline").

## What we are looking for

* Bug fixes with regression tests
* Tests that improve coverage of real behavior
* UI improvements that follow the design system and Nielsen heuristics
* Domain and service refactors that respect layering
* Documentation that helps users and contributors

## What to avoid

* Large unrelated drive-by refactors mixed with feature work
* Business logic in controllers or domain imports from infrastructure
* New domain concepts in `model/` instead of `project/`
* User-facing features without tests or user-guide updates
* Committed credentials, API keys, or local override files

## License

By contributing, you agree that your contributions will be licensed under the
same license as the project. See [LICENSE](LICENSE) (GNU GPL v2).

## Questions

Open a [GitHub issue](https://github.com/vepo/video-creator/issues) for
questions, bug reports, or feature discussion. For security vulnerabilities,
see [SECURITY.md](SECURITY.md).
