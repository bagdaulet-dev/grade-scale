# Architecture

```text
HTML/CSS/JavaScript → Spring MVC controller → repository → MySQL
                            ↓                    ↓
                      grade calculator       JdbcTemplate
```

The calculator is independent of HTTP and storage. It uses `BigDecimal` throughout;
intermediate weighted points are not rounded. Final displayed grades round half-up
to two decimal places. Target recommendations round upwards.

Two storage profiles implement the same small repository interface. The default
`preview` profile uses a synchronized, bounded in-memory store. It starts with only
Java and resets after restart. The `mysql` profile uses `JdbcTemplate`, prepared
parameters, a transaction manager, and Spring Session JDBC. It persists session
cookies' server-side data as well as courses, so a still-valid browser session can
resume after an application restart.

The initial schema has two domain tables: `courses` and `assessments`. A small
`workspaces` table supports owner locking and expiry. Spring Session adds its own
two session tables. Foreign keys cascade deletion. A transaction locks the course
before adding or changing assessments, preventing concurrent requests from pushing
its total weight above 100%. Creating courses locks the workspace before enforcing
the course limit. MySQL checks also enforce basic score and weight ranges.

Workspace data expires within 24 hours. MySQL reads exclude expired workspaces;
an hourly job removes them and their courses. Spring Session cleans expired sessions.
The preview removes expired entries during access. There are no long-term user
accounts, shared courses, external services, file uploads, or analytics.

Source is kept in one package because this project is intentionally small. Request
and response records live in `Models.java`; separate services or package layers
can be introduced when the project grows. The frontend is served from conventional
Spring static resources, so cookies and API calls remain on the same origin.

The database connection implementation deliberately avoids an additional connection
pool. This is suitable for a small local learning project. A higher-traffic service
should configure a bounded pool and shared rate limiting before scaling.
