# Contributing

Start with the [README](README.md), then run the native preview. Keep changes small
and explain the behavior being changed. Use fictional courses and scores in tests.

```bash
./mvnw fmt:check verify
npm ci --ignore-scripts
npm audit
npm run format:check
BROWSER_CHANNEL=chrome npm run test:browser
```

Use `./mvnw fmt:format` and `npm run format` before submitting a change. The browser
journey expects the application to be running separately. MySQL-specific checks
need a dedicated database and `TEST_STORAGE=mysql`.

For calculations, add a case at the relevant boundary and explain rounding. For
persistence, include owner isolation and transaction behavior. For visible UI
changes, check keyboard operation, reduced motion and mobile layouts.

Do not submit real student data, `.env`, session cookies, private keys, dependency
folders or generated JARs. Report vulnerabilities through [SECURITY.md](SECURITY.md).
