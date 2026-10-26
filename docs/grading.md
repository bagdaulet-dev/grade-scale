# Grade calculations

`BigDecimal` keeps decimal scores and weights exact through the intermediate
calculation. Inputs allow two decimal places. Weights must be positive and total
at most 100; scores range from 0 to 100.

For each assessment, earned points = score × weight / 100. A complete course's
result is the sum of these points. Before the course is complete, the current
average is earned points × 100 / completed weight and remains **provisional**.
An empty course has no grade.

For example, 80 at 40% weight earns 32 points. Adding 90 at 60% weight produces
86 overall, a B+. With only the first assessment entered, the provisional average
is 80, with 32 points earned and 60% still available.

Displayed results round half-up to two decimals before the letter is assigned.
A result of 94.995 therefore displays 95.00 and receives A. Boundaries come from
`GET /api/scale`; the frontend uses the same values as the backend.

| Letter | Inclusive minimum | Upper limit   |
| ------ | ----------------- | ------------- |
| A      | 95                | 100 inclusive |
| A-     | 90                | below 95      |
| B+     | 85                | below 90      |
| B      | 80                | below 85      |
| B-     | 75                | below 80      |
| C+     | 70                | below 75      |
| C      | 65                | below 70      |
| C-     | 60                | below 65      |
| D+     | 55                | below 60      |
| D      | 50                | below 55      |
| F      | 0                 | below 50      |

For a target T with E earned points and R remaining weight, required score is
`(T - E) × 100 / R`. Recommendations round **up** to two decimals. Above 100 is
unreachable; at or below zero is already secured. With no remaining weight, the
planner rejects the request because the course is complete.

The scale is a project reference. It does not claim to represent a school's policy.
