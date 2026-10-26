# Testing guide

Calculation tests use TestNG without starting a server. API tests start Spring Boot
on a random local port and use Java's HTTP client with independent cookie jars.
The same API checks can run against the preview repository or real MySQL.

```bash
./mvnw fmt:check verify
TEST_STORAGE=mysql MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 \
MYSQL_DATABASE=grade_scale_test MYSQL_USER=grade_scale \
MYSQL_PASSWORD='<test-password>' ./mvnw test
```

Use a dedicated test database. Schema creation and expiry tests write to it. Never
run these tests against a production database. The MySQL-only restart test is
explicitly skipped in the preview-only run.

Run the browser journey with the app started in another terminal:

```bash
BROWSER_CHANNEL=chrome npm run test:browser
```

It uses synthetic data in a fresh browser context and checks creation, editing,
validation, target planning, deletion, reload, session isolation and layout widths.
Screenshots are regenerated under `docs/screenshots/`.

For a regression, first make the focused test fail on the old behavior, fix the
cause, then run the relevant suite. Test outcomes are recorded in
[verification.md](verification.md). CI uses Java 21 and MySQL 8.4.

CI runs a clean preview test pass, then a MySQL pass without deleting JaCoCo execution
data. This produces combined coverage for the two repository implementations.
The current suite has 58 TestNG cases including parameterized boundaries; preview
mode skips the MySQL-only restart/expiry case. MySQL mode runs all 58 cases.
Coverage reports are at `target/site/jacoco/index.html` and in the GitHub verification
artifact. Tests have bounded HTTP waits and close their clients after each class.

`python3 scripts/check-openapi.py` compares all 13 controller operations with the
OpenAPI document and resolves internal references. It checks contract drift; it is
not a full OpenAPI semantic validator. `python3 scripts/audit-maven.py` audits the
resolved public dependency coordinates against OSV and writes a report under target.
