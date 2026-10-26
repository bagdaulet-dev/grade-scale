#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -B -ntp fmt:check verify
npm ci --ignore-scripts
npm audit
npm run format:check
python3 scripts/check-openapi.py
python3 scripts/audit-maven.py
node --check src/main/resources/static/export.js
node --check src/main/resources/static/app.js
node --check scripts/browser-smoke.mjs
echo "Source checks passed. Start the app, then run: BROWSER_CHANNEL=chrome npm run test:browser"
