# Verification

The checks below describe local results, not a claim that a remote GitHub workflow
has run. No repository is connected and nothing has been uploaded.

## Commands

```bash
./mvnw fmt:check verify
npm ci --ignore-scripts
npm audit
npm run format:check
BROWSER_CHANNEL=chrome npm run test:browser
```

`TEST_STORAGE=mysql` enables the real-MySQL API matrix and the restart/expiry test.
Without that variable, the MySQL-only test is explicitly skipped.

The calculation tests cover the documented 86% example, incomplete and empty
courses, decimal weights, weight overflow, letter boundaries, and final-score targets.
HTTP tests exercise create/edit/delete, CSRF, invalid data, owner isolation,
request limits, security headers, and simultaneous assessment creation.

The MySQL-only test also verifies that a session can reopen its course after
restarting the application, and that expired workspace data becomes inaccessible
and is removed by cleanup.

The browser journey covers example creation, live grade changes, overweight
validation, completing a course, cancelling and confirming removal, scale help,
reload persistence within a session, a separate empty browser session, and
320px/390px/768px layouts. Screenshots are saved in `docs/screenshots/`.

## Results on 26 October 2026

- **43 TestNG tests passed against MySQL 8.4**, with no failures or skips.
- Boundary cases cover both sides of every five-point grade band, 0 and 100,
  and rounding before assigning a letter. `GET /api/scale` publishes all 11 bands.
- MySQL session recovery across an app restart and expired-data cleanup passed.
- A temporary-copy mutation changing the 100% weight check was caught by tests;
  the working source was not changed by that experiment.
- Java source formatting, JavaScript syntax, and Prettier checks passed.
- npm audit reported no known vulnerabilities. OSV reported no advisories for
  the 59 resolved Maven dependency coordinates checked at verification time.
- The Docker image built successfully and ran under Java 21. Its MySQL health,
  frontend response and 11-band scale were verified; Docker health was healthy.
- Manual Chrome checks passed for example creation, score editing/recalculation,
  server rejection of excessive weights, target planning, scale help, and reload.
  Browser logs contained no unexpected JavaScript warnings/errors after reload.
- Layout checks at 320px, 390px and 768px found no horizontal overflow.
- Desktop and mobile screenshots show the requested A- result for 90.13%.
- The repeatable Playwright script is included for future CI; its complete
  automated deletion journey was not executed in this local verification pass.
- Docker and its test containers were stopped after verification. The native
  preview remains available on localhost:8081 with a 192 MB Java heap cap.
- No GitHub repository was created, no remote configured, and nothing uploaded.

The GitHub workflows are prepared files. They have not run on GitHub. Advisory
checks describe a point-in-time result and do not certify the absence of flaws.
