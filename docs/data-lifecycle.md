# Private workspace data

Grade_scale uses anonymous browser sessions. A course belongs to the server-derived
workspace associated with its session cookie. Sharing a course URL does not share
access; another browser gets no permission to that course.

Preview data lives in memory and resets on restart. MySQL stores courses, assessments
and valid sessions across app restarts. Domain data expires within 24 hours, and
clearing cookies loses access to anonymous records. The app does not provide
registration, password recovery or official student record retention.

Use **Export course** to download the displayed course, assessments, result and grade
scale as JSON before expiry. The file contains no session credentials or owner hash.
Export does not extend server retention and there is no import operation in this
version. Store any downloaded file according to your own privacy needs.

Deleting a course removes its assessments. Deleting an assessment recalculates the
result. These operations require confirmation in the interface and the same ownership
and CSRF checks as other changes. A MySQL volume is persistent storage, not a backup.
