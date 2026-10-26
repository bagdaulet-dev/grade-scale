# Troubleshooting

## Port 8081 is occupied

Stop the other Grade_scale process before starting Docker, or choose another native
port: `java -jar target/grade-scale.jar --server.port=8083`. The page and API must
use the same origin. For browser tests, set `BASE_URL=http://localhost:8083/`.

## Java compilation reports an unsupported release

Run `java -version` and `./mvnw -version`. Both should select Java 21 or newer.
Changing PATH or JAVA_HOME in an already-open terminal may be necessary.

## Docker cannot connect

Start Docker Desktop and wait for its engine. `docker compose stop` stops the
containers; quitting Docker Desktop also releases the virtual machine's memory.
Native preview mode does not need Docker.

## MySQL startup fails

Inspect `docker compose logs db` and `docker compose logs app`. Compose's database
hostname is `db`; a native Java process uses a host/port reachable from your Mac.
The app requires a dedicated database user and an explicit password. The app
container is deliberately not given the root password.

## A form asks for a refresh

The browser session or CSRF token may have expired. Refresh, then inspect your
course before retrying a change. A timed-out write may already have succeeded.
The app does not automatically retry writes.

## Courses disappeared

Preview mode resets on app restart. All workspace data expires within 24 hours.
Clearing cookies loses access to anonymous data. MySQL persists valid sessions
across app restarts, but does not provide long-term accounts or recovery by email.

## Docker data survives changed sample passwords

MySQL initialization variables apply to a new data directory. An existing named
volume retains its users and passwords. Do not delete a real database volume to
change credentials; use the normal MySQL administration procedure and keep backups.
