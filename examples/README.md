# API examples

Start Grade_scale and request a session:

```bash
curl -c /tmp/grade-scale.cookies http://localhost:8081/api/session
```

Copy the returned CSRF token into a temporary shell variable. Use a fresh token
for your own session; never commit it. Then create a course:

```bash
curl -b /tmp/grade-scale.cookies -c /tmp/grade-scale.cookies \
  -H "X-CSRF-TOKEN: $GRADE_SCALE_CSRF" -H 'Content-Type: application/json' \
  --data-binary @examples/course.json http://localhost:8081/api/courses
```

Use the returned course ID to add `assessment.json` at
`/api/courses/<course-id>/assessments`. Read `/api/courses/<course-id>/result` with
the same cookie file. See [requests.http](requests.http) for an ordered session,
course, assessment and result journey. All example values are fictional.
