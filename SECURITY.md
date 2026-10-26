# Reporting security issues

Use the repository's **Security → Report a vulnerability** page for private reports:
https://github.com/bagdaulet-dev/grade-scale/security/advisories/new

Include affected endpoints, a minimal reproduction using fictional data, the
expected access boundary, and the observed behavior. Remove credentials, cookies,
CSRF tokens and personal information. Avoid public issues for an exploitable flaw.

This project is a local portfolio application with anonymous, expiring workspaces.
It is not an official student records system. Its controls and deployment limits
are described in [docs/security.md](docs/security.md). Only the current main branch
is maintained; no response-time or security certification is promised.
