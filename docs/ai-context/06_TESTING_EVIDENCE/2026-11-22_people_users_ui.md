# Users interface verification — 22 November 2026

Verified on the P09 working tree based on `0f8737d` in the canonical checkout.

| Check | Observed result |
| --- | --- |
| `npm run lint -- --max-warnings 0` | Passed. |
| `npm run typecheck` | Passed, including the regenerated API types and browser test. |
| `npm test` | All 14 tests passed across session, routes and People. |
| `npm run build` | Passed. Rollup retained the existing warnings about Zod comment annotations. |
| `npm run test:browser -- tests/browser/people.spec.ts` | Chrome passed the People journey in 19.5 seconds overall. |

The five People client cases exercise field validation, permission gating,
account creation, one-time password dismissal, absence of passwords from Query
and Mutation cache data, duplicate email recovery, unknown write outcomes,
role changes, deactivation and Escape dismissal with focus restoration.
The initial screen test failed while the route was absent. A subsequent test
caught validation text changing the input's accessible name. Moving the error
outside the label retained its accessible description and resolved that failure.
Two unsupported Testing Library options were corrected after TypeScript rejected
them. The final checks passed.

The browser used a dedicated backend process on port 18089 and Vite on 5173,
with synthetic fixtures in the owned native test target. Only that browser
process had its login limit raised to 100 for the repeated first-use flows.
Its private fixture JSON supplied a new first-use administrator, a unique name
prefix and two tenant role names through `BAITI_PEOPLE_BROWSER_FIXTURE`.
The administrator and new user both signed in and replaced their generated
passwords through the real UI. The new user initially lacked `USER_MANAGE` and
received 403 from the Users API. The administrator then changed the role and
deactivated the account. Reload rejected its refresh, and a fresh sign-in failed.

Escape restored focus to Add user. The final 375×812 view with reduced motion
enabled retained the Users heading and had no page-wide horizontal overflow.
The default desktop view completed the workflow. These checks cover the listed
keyboard and responsive behaviours. Screen-reader operation remains unverified.
The browser output is in the ignored
[local log](../../../backend/.local/p09-browser.log). Fixture-owned accounts,
role grants, roles and newly inserted permissions were removed after the run.
The verification backend and its PostgreSQL cluster were stopped afterwards.

The full P09 J1 acceptance still awaits resident registration and unit linkage.
