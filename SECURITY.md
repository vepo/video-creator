# Security Policy

## Supported versions

Security fixes are applied to the active development branch (`main`). Older
releases are not maintained separately unless noted in release tags.

| Version | Supported |
|---------|-----------|
| `main` (latest) | Yes |
| Older tags / snapshots | Best effort |

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, use one of these channels:

1. **GitHub private vulnerability reporting** — On the repository page, open
   **Security → Report a vulnerability** (if enabled for the repository).
2. **Direct contact** — Message [@vepo](https://github.com/vepo) on GitHub with
   details and steps to reproduce.

Include as much of the following as you can:

* Description of the vulnerability and potential impact
* Steps to reproduce
* Affected endpoints, files, or configuration
* Proof of concept, if available
* Suggested fix, if you have one

## What to expect

* **Acknowledgment** within a reasonable timeframe (typically a few business days).
* **Assessment** of severity and affected scope.
* **Updates** as the issue is triaged and fixed.
* **Credit** in the fix release notes if you wish (coordinated disclosure).

## Scope

The following are in scope for security reports:

* The Video Creator application (Quarkus backend, REST API, WebSockets, Qute UI)
* Authentication, authorization, and session handling (when present)
* File upload, media storage, and download paths
* Server-side command execution related to preview and export (`melt`, FFmpeg)
* Dependency vulnerabilities with a plausible exploit path in this application

The following are generally **out of scope**:

* Issues in third-party tools (MLT, MongoDB, FFmpeg) unless exploitable through
  Video Creator's integration
* Denial of service from intentionally large uploads within documented limits
* Social engineering or physical access attacks
* Vulnerabilities in dependencies already fixed in `main` but not yet released

## Safe disclosure

We ask reporters to allow reasonable time to investigate and patch before public
disclosure. We support coordinated disclosure and will work with you on timing.

## Security best practices for deployments

If you self-host Video Creator:

* Do not expose the instance to the public internet without appropriate access
  controls.
* Run MongoDB on a private network; do not use default credentials in production.
* Keep MLT, FFmpeg, and the JVM runtime patched.
* Do not commit `application.properties` overrides containing secrets.
* Review upload size limits (`quarkus.http.limits.max-body-size`) for your
  environment.
