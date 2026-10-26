# Containers and delivery

The supported default is a local application on `127.0.0.1:8081`. Docker Compose
runs the Java app and MySQL with a private database network and named data volume.
It limits both services to 384 MB each. The app runs unprivileged with a read-only
root filesystem and temporary scratch space.

## Build and run locally

```bash
cp .env.example .env
docker compose up --build -d --wait
curl --fail http://localhost:8081/api/health
```

The health endpoint checks MySQL connectivity in MySQL mode. Initial startup
creates the documented schema and session tables. It does not modify existing
tables to match future schema changes; introduce reviewed Liquibase SQL changes
before extending a database already containing data.

`docker compose stop` keeps data. Quit Docker Desktop to release its VM memory.
Avoid deleting the named database volume unless its contents are disposable.

## GitHub delivery

Pull requests and pushes to main run formatting, TestNG with real MySQL, frontend
checks, browser tests and a container build. A successful main build can publish
an immutable commit-tagged image to GitHub Container Registry. No deployment server
is configured by this repository. A separate manual workflow exports an image as
an artifact when a registry is not needed.

## Before running on another server

Supply HTTPS through a trusted proxy, set `SECURE_COOKIES=true`, use unique database
credentials, and restrict network access. Preserve MySQL's data volume and keep
backups outside it. Verify database certificates for a remote connection. Configure
forwarded-header trust only for a proxy that you control and can restrict at the
network boundary.

This is a small, anonymous workspace application. Data expires within 24 hours,
there is no account recovery, and rate limiting is process-local. It is suitable
for learning and a portfolio demonstration; operating a multi-user student records
service would require different authentication, retention, availability and privacy
decisions.

## Registry access

The image includes OCI source labels linking it to this repository. A first GHCR
publication may be private even for a public code repository. To allow anonymous
pulls, the owner can choose Public in the package's visibility settings; otherwise
a consumer must authenticate Docker with permission to read that package. Never
store a registry token in this repository. See [GitHub's package visibility guide](https://docs.github.com/en/packages/learn-github-packages/configuring-a-packages-access-control-and-visibility).

Use the exact commit tag shown by a successful workflow. The container publication
job runs only after verification succeeds on a main-branch push. It has package-write
permission; the verification job retains read-only repository access.
