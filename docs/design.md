# Interface design

The course workspace uses a quiet sidebar and a two-column assessment/result
layout. The grade dial is the main visual; the rest of the page supports editing
and reading the calculation.

| Role                       | Color     |
| -------------------------- | --------- |
| Plum text and result panel | `#302338` |
| Lavender surfaces          | `#e8e1ef` |
| Peach grade arc            | `#f4ae8c` |
| Mint progress accent       | `#c8ddca` |
| Page background            | `#fbf9fc` |

Bricolage Grotesque carries the display typography; DM Sans handles controls and
body text. Both fonts are bundled locally, with their licenses. No font CDN is
needed while using the app.

The SVG dial reacts to the current average. The weight bar represents entered
assessment weights, while the final-score planner explains remaining work.
Motion is brief and disabled for reduced-motion preferences. Mobile layouts stack
the result below assessments. Native dialogs, visible focus, explicit labels,
inline errors and deletion confirmation support keyboard use.

Screenshots are in [screenshots/](screenshots/). Keep the palette and hierarchy
consistent when extending the interface; keep implementation details in developer
documentation rather than adding them to ordinary grade-entry forms.
