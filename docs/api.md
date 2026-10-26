# API contract

Import [openapi.json](openapi.json) into an OpenAPI-compatible client, or use the
[ordered HTTP examples](../examples/requests.http).

Base URL: `http://localhost:8081/api`. Responses are JSON. Mutation bodies must
use `Content-Type: application/json`; keep bodies below 16 KB. Course IDs and
assessment IDs are UUIDs. There are no accounts: your cookie identifies a private
browser workspace. Never expose session cookies or tokens in source code.

## Start a session

```bash
curl -c /tmp/grade-scale-cookies http://localhost:8081/api/session
```

The response includes `csrfToken`, `csrfHeader` (`X-CSRF-TOKEN`), and `storage`
(`preview` or `mysql`). Use the cookie file on subsequent requests and add the
returned token as a header for POST, PUT, PATCH and DELETE. The browser interface
does this automatically. Refresh the page if its session expires.

| Method | Endpoint                        | Result                                                 |
| ------ | ------------------------------- | ------------------------------------------------------ |
| GET    | `/health`                       | App health, including MySQL connectivity in MySQL mode |
| GET    | `/session`                      | Session cookie, CSRF token, storage mode               |
| GET    | `/courses?page=1`               | Up to 20 courses with pagination metadata              |
| POST   | `/courses`                      | Create a course, 201                                   |
| GET    | `/courses/{id}`                 | Course, assessments, calculated result                 |
| PUT    | `/courses/{id}`                 | Replace name and code                                  |
| DELETE | `/courses/{id}`                 | Delete course and assessments, 204                     |
| POST   | `/courses/{id}/assessments`     | Add assessment, 201                                    |
| PATCH  | `/assessments/{id}`             | Change supplied fields                                 |
| DELETE | `/assessments/{id}`             | Remove assessment, 204                                 |
| GET    | `/courses/{id}/result`          | Grade result                                           |
| GET    | `/courses/{id}/target?score=90` | Required average on remaining work                     |

## Requests

Course: `{"name":"Software Engineering","code":"CS 240"}`.
Name is required and at most 80 characters; code is required as a JSON field but
may be empty, up to 20 characters.

Assessment: `{"name":"Midterm exam","score":84,"weight":30}`.
Name is required and up to 60 characters; score is 0–100; weight is above 0 and
at most 100. Scores and weights allow up to two decimal places. Total course
weight cannot exceed 100. A PATCH may contain any subset of these fields; omitted
or null fields retain their previous value. Empty names are rejected.

## Grade result

```json
{
  "score": 84.0,
  "letterGrade": "B",
  "completedWeight": 30,
  "remainingWeight": 70,
  "earnedPoints": 25.2,
  "provisional": true,
  "contributions": [
    {
      "id": "assessment-uuid",
      "name": "Midterm exam",
      "score": 84,
      "weight": 30,
      "points": 25.2
    }
  ]
}
```

Empty courses return `score: null`, `letterGrade: null`, and `provisional: true`.
For a complete course, score is the sum of weighted points. For an incomplete
course, score is the normalized average of graded work. It is not a final grade.

Target planning returns `target`, `requiredScore`, `remainingWeight`, and
`status`: `REACHABLE`, `UNREACHABLE`, or `SECURED`. Required scores round upwards
to two decimals so rounding does not recommend an insufficient result. Target
planning requires remaining weight. It assumes all remaining work contributes
the specified total weight, with the same score range of 0–100.

## Errors and retries

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Assessment weights cannot exceed 100% in total."
  }
}
```

400 means malformed input, 403 a missing/expired CSRF token, 404 a missing or
foreign-owned resource, 405 an unsupported method (with an Allow header), 406 an unsupported response format, 413 an oversized body, 415 an
unsupported content type, 422 invalid fields/weights, 429 rate limiting, and 503
a temporary service issue. Error bodies do not include SQL or stack traces.

POST creates a new resource and is not idempotent. Do not automatically retry an
uncertain POST: refresh the course list first. PUT replaces values and PATCH sets
specified values; repeated identical updates keep the same state. Repeating a
DELETE of an already removed resource returns 404. The browser never automatically
retries writes. MySQL writes use transactions and lock the course while checking
and changing its assessment weights.

The grading scale is returned by `GET /api/scale`: A ≥95, A- ≥90, B+ ≥85,
B ≥80, B- ≥75, C+ ≥70, C ≥65, C- ≥60, D+ ≥55, D ≥50, F below 50.
Lower bounds are inclusive; the next higher band starts at its own lower bound.
The interface reads these exact boundaries from the backend.

Course and assessment names reject control characters as well as blank values.
The interface can export the visible course detail and grade scale to a JSON file;
exports contain no cookies, CSRF tokens or owner identifiers. Export is a local
download and does not create another server-side record.
