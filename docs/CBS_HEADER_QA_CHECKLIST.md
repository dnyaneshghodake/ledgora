# CBS Header — Tester / QA Checklist

Companion to [CBS_HEADER_ARCHITECTURE.md](CBS_HEADER_ARCHITECTURE.md).

Use this checklist when verifying the Finacle-grade header after login.

---

## Visual Checks (desktop >= 1024px)

| # | Check | Expected |
|---|---|---|
| 1 | SOL ID visible | Shows branchCode (e.g. 1001) |
| 2 | Branch name visible | Shows (branchName) next to SOL |
| 3 | CCY visible | Shows INR or baseCurrency if set |
| 4 | Environment badge | DEV / UAT / PROD with correct color |
| 5 | Business date | Matches tenant current_business_date |
| 6 | Day status | OPEN / DAY_CLOSING / CLOSED matches DB |
| 7 | Ledger health | HEALTHY / WARNING / CORRUPTED |
| 8 | Session timer | Starts at 30:00, counts down each second |
| 9 | Session timer warning | Turns amber at 5 minutes or less |
| 10 | Session timer critical | Turns red and blinks at 2 minutes or less |
| 11 | Session timer reset | Any click/key/scroll resets to 30:00 |
| 12 | Maker/Checker badge | Shows MAKER (green), CHECKER (blue), or DUAL (purple) per role |
| 13 | No badge for non-M/C | Admin/Teller/Customer: no mode badge shown |
| 14 | Pending approvals | Bell + count visible for Admin/Manager/Checker when > 0 |
| 15 | Shortcut bar | Alt+D/T/C/A/S/P hints shown below header |
| 16 | Shortcut bar Approvals | Alt+P only visible for Checker/Admin/Manager |
| 17 | No overlap | Left/center/right sections do not collide at 1366x768 |

---

## Keyboard Shortcut Checks

| # | Press | Result |
|---|---|---|
| 1 | Alt+D | Navigates to /dashboard |
| 2 | Alt+T | Navigates to /transactions/deposit |
| 3 | Alt+C | Navigates to /customers |
| 4 | Alt+A | Navigates to /accounts |
| 5 | Alt+S | Toggles sidebar open/closed |
| 6 | Alt+P | Navigates to /approvals |

---

## Responsive Checks

| # | Breakpoint | Expected |
|---|---|---|
| 1 | 991px or less | Center header hidden, shortcut bar hidden, mode badge hidden, currency hidden |
| 2 | 767px or less | Brand text hidden, tenant info hidden, session timer hidden |
| 3 | Sidebar collapsed | Shortcut bar expands to full width |

---

## Login-Specific Checks

| # | Check | Expected |
|---|---|---|
| 1 | maker1 / maker123 | Green MAKER badge visible |
| 2 | checker1 / checker123 | Blue CHECKER badge visible |
| 3 | admin / admin123 | No mode badge (has broader role, not exclusively maker/checker) |
| 4 | customer1 / cust123 | No mode badge, no approvals bell, no Alt+P hint |
| 5 | Logo image missing | Fallback bank icon (bi-bank2) shows instead of broken image |

---

## Session Timeout Flow

1. Login with any user
2. Observe timer starts at 30:00
3. Wait idle — timer counts down
4. At 5:00 remaining, timer turns amber (warning state)
5. At 2:00 remaining, timer turns red and blinks (critical state)
6. Move mouse or press any key — timer resets to 30:00
7. If timer hits 0:00 without activity, browser redirects to /login?expired=true
