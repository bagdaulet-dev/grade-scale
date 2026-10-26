# Security boundaries

This is a local portfolio application with anonymous, session-owned data. It is
not a school records system and should not hold official student records or PII.
Use fictional course examples for demonstrations.

- Browser sessions use HttpOnly and SameSite=Strict cookies. MySQL stores sessions;
  preview sessions live in memory. Clearing cookies loses access to that workspace.
- Every course and assessment read, write, and delete checks server-derived ownership.
  Owners are SHA-256 hashes of unpredictable server session identifiers. IDs supplied
  by clients never determine ownership. Foreign resources return 404.
- Changes require Spring Security CSRF validation. There is no wildcard CORS policy.
- Bodies are bounded to 16 KB before JSON parsing. Names, numeric ranges, decimal
  places, item limits, pagination and aggregate weights are validated on the server.
- Queries use prepared parameters. User text is rendered with `textContent`; there
  are no dynamic HTML templates, inline scripts, runtime CDNs, or API keys.
- CSP, frame restrictions, MIME sniffing protection, referrer and permissions headers
  are configured. API responses use `no-store`. Error bodies omit SQL and traces.
- A per-IP limiter permits 240 API requests per minute per application process.
  This is best effort, designed for one local process. Do not trust forwarded client
  addresses without configuring and restricting an actual reverse proxy.
- The web container runs as an unprivileged user with a read-only filesystem and
  dropped capabilities. Compose binds the app to loopback and does not publish MySQL.
- Database credentials are supplied via environment variables. `.env`, private keys,
  build output, and dependencies are ignored. `.env.example` values are public local
  examples. The root database password is never passed to the app container.
- MySQL connections require encryption by default. `VERIFY_IDENTITY` plus trusted
  database certificates is appropriate for remote database hosts.

For any deployment beyond your own computer, supply HTTPS at a trusted reverse proxy,
set `SECURE_COOKIES=true`, use unique database credentials, restrict network access,
and decide how long data should be kept. HSTS is applied by Spring Security on secure
requests. Do not set proxy-trust flags for arbitrary incoming traffic. Back up and
restore the database deliberately; a named volume is not a backup.

The initial schema is idempotent (`CREATE TABLE IF NOT EXISTS`), not a migration
history. Use reviewed Liquibase SQL changes before changing an existing schema.
Dependency checks detect known advisories, not every vulnerability. This document
describes concrete controls and limitations; it is not a security certification.

## Repository checks

GitHub Actions references are pinned to official commit hashes. Dependabot tracks
Maven, npm, Actions and Docker updates. The CI audit uses npm's lockfile audit and
OSV queries for the resolved Maven runtime/test coordinates. Build plugins and base
images still need their own upgrade review; no advisory scan proves all code safe.

The JSON export contains only the displayed course, assessments, result and grading
scale. It omits session cookies, CSRF tokens and internal owner hashes. Keep downloaded
course files private when they contain information you want to retain locally.
