# Verification

[GitHub checks](https://github.com/bagdaulet-dev/grade-scale/actions/workflows/ci.yml)
provide the current branch result. This file records the checks and their scope.

## Backend

The suite contains **58 TestNG cases**. A local preview pass runs 57 and explicitly
skips the MySQL-only restart/expiry case. The CI MySQL pass runs all 58. An earlier
local MySQL pass verified the original 43 cases, including session recovery and
expiry; the expanded MySQL matrix is part of the repository workflow.

Coverage includes exact decimal arithmetic, all letter boundaries, final-score
rounding, provisional results, malformed JSON, owner isolation, CSRF on every write
method, aggregate-weight rollback, course/assessment capacities and concurrency.

The review reproduced and fixed missing Allow headers, inconsistent 406 JSON errors,
and acceptance of control characters. New regression checks fail on the old behavior.
An isolated-copy mutation of the weight boundary was caught by calculation tests.

## Browser

The complete automated Chrome journey passed locally, including course creation,
assessment editing, server weight validation, target planning, cancel/confirm delete,
JSON export, selected-course reload, a separate empty session, and responsive widths
320/390/768. Screenshots were regenerated in `docs/screenshots/`.

## Tooling and supply chain

Java formatting, frontend formatting, JavaScript syntax and OpenAPI route/reference
checks passed. npm audit reported no known vulnerabilities; OSV returned no advisories
for 59 resolved Maven coordinates at local verification time. CI repeats the audits.
Actions are pinned to official commit hashes and dependency updates are scheduled.

JaCoCo reports combine preview and MySQL runs in CI. Review the generated artifact
for coverage instead of assuming a fixed percentage. No coverage score or dependency
scan certifies the absence of flaws.

The Java 21 container was previously built and smoke-tested locally with MySQL.
CI rebuilds the final source. Docker Desktop remains stopped on the development Mac;
the lightweight native preview needs only Java. No production server is deployed.
