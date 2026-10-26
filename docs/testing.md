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
