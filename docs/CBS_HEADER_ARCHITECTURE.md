# CBS Header Architecture (Finacle-Grade)

This document describes the **Finacle-style CBS header** implemented in Ledgora.

It covers:

- Header layout and information model
- Session attributes required
- CSS architecture (base vs enterprise theme)
- Client-side behavior (session timeout + shortcuts)
- Responsive rules and layout offsets
- Tester / QA checklist

> Primary implementation files:
>
> - Markup: `src/main/webapp/WEB-INF/jsp/layout/header.jsp`
> - Base CSS: `src/main/webapp/resources/css/style.css`
> - Enterprise theme overrides: `src/main/webapp/resources/css/enterprise-theme.css`
> - JS behavior: `src/main/webapp/resources/js/app.js`

---

## 1) Layout Model

The UI follows a CBS-standard fixed layout:

- **Fixed Top Header** (`.cbs-header`)
- **Fixed Sidebar** (`.cbs-sidebar`)
- **Optional fixed Keyboard Shortcut Bar** (`.cbs-shortcut-bar`) below the header
- **Scrollable Main Content** (`.cbs-main`)

### Zoning

The header uses a 3-zone flex layout:

1. **Left**: navigation toggle + brand
2. **Center**: operational context (branch/SOL, currency, environment, business date, day status, ledger health)
3. **Right**: tenant context + session timer + approvals + maker/checker mode + user identity + logout

This matches typical Finacle/CBS conventions: operational context is always visible and separated from user controls.

---

## 2) Header Information Architecture

### 2.1 Operational Context (Center)

Displayed elements:

- **SOL ID (branch code)** as primary identifier
- **Branch name** as secondary label
- **CCY (base currency)**
- **Environment badge** (DEV/UAT/PROD)
- **Business date** (banking business date, not system date)
- **Business day status** (OPEN / DAY_CLOSING / CLOSED)
- **Ledger health** (HEALTHY / WARNING / CORRUPTED)

These fields are intentionally **dense** and **read-only**.

### 2.2 User + Security Context (Right)

Displayed elements:

- **Tenant name** (and optional tenant switch dropdown for MULTI scope)
- **Session timeout countdown** (PCI-DSS / RBI control)
- **Pending approvals** indicator (role gated)
- **Maker/Checker mode badge** (prominent)
- Username + role badges
- Logout

---

## 3) Required Session Attributes

The header reads values from `sessionScope`.

### Required / used attributes

| Attribute | Purpose | Example |
|---|---|---|
| `username` | Logged-in user | `teller1` |
| `branchCode` | SOL ID / branch identifier | `1001` |
| `branchName` | Branch name (secondary label) | `Mumbai Main` |
| `environment` | Environment badge | `DEV` / `UAT` / `PROD` |
| `businessDate` | Current business date | `2026-03-10` |
| `businessDateStatus` | Day status | `OPEN` |
| `ledgerHealth` | Ledger integrity state | `HEALTHY` |
| `tenantName` | Tenant display | `Ledgora Main Bank` |
| `tenantScope` | Enables tenant switch | `SINGLE` / `MULTI` |
| `availableTenants` | Tenant list for switch | list of tenants |
| `pendingApprovals` | Bell badge count | `3` |
| `isMaker` / `isChecker` | Mode badge + role display | boolean |

### Newly introduced / recommended

| Attribute | Purpose | Default |
|---|---|---|
| `baseCurrency` | CCY display | `INR` |

If `baseCurrency` is not set, the UI shows **INR**.

---

## 4) CSS Architecture

### 4.1 Base Styles (`style.css`)

`style.css` defines the functional layout and components.

Key variables:

```css
:root {
  --cbs-header-height: 54px;
  --cbs-shortcut-bar-height: 24px;
  --cbs-sidebar-width: 260px;
  --cbs-footer-height: 40px;
}
```

Layout offsets:

- `.cbs-main` top margin = `header + shortcut bar`
- `.cbs-sidebar` top offset = `header + shortcut bar`
- `.cbs-eod-banner` top offset = `header + shortcut bar`

This ensures fixed elements never overlap.

### 4.2 Enterprise Theme Overrides (`enterprise-theme.css`)

`enterprise-theme.css` is loaded **after** bootstrap and base CSS.

It:

- Re-maps `--cbs-*` colors to a conservative banking palette
- Enforces compact density
- Applies dark-header “pill” styling to header items
- Overrides warning/critical styles to match dark background

---

## 5) Client-Side Behavior (`app.js`)

### 5.1 Session timeout countdown

- Starts at **30:00**
- Warns at **5 minutes** (`.cbs-session-warning`)
- Critical at **2 minutes** (`.cbs-session-critical` + blink)
- Resets on user activity:
  - click, keypress, scroll, mousemove
- At 0:00 redirects to `login?expired=true`

> Note: this is a **front-end guard**. Server-side session timeout must still be configured in Spring Security/container.

### 5.2 Keyboard shortcuts (Finacle-style)

`Alt+Key` navigation:

| Shortcut | Action |
|---|---|
| `Alt+D` | Dashboard |
| `Alt+T` | New transaction (default Deposit) |
| `Alt+C` | Customers |
| `Alt+A` | Accounts |
| `Alt+S` | Toggle sidebar |
| `Alt+P` | Approvals |

The shortcut bar in the UI mirrors these.

---

## 6) Responsive Rules

CBS UI is desktop-first.

On smaller screens (≤ 991px / ≤ 1024px depending on theme rules), the UI:

- Hides center header context (to avoid overlap)
- Hides shortcut bar
- Hides maker/checker mode badge
- Hides currency context
- Resets `.cbs-main` margin-top back to header-only
- Resets sidebar / overlay / banners to header-only top offset

This avoids UI collisions on constrained widths.

---

## 7) Operational Notes / Standards Alignment

This header is designed to match common CBS expectations:

- SOL (branch) and business date are always visible
- Maker/Checker mode is explicit to reduce operational errors
- Session countdown supports compliance expectations
- Dense, high-signal header with clear separators

---

## 8) Future Enhancements (Optional)

> See Section 9 below for a full tester/QA checklist.

If you want to go further toward Finacle parity:

- Show **transaction scroll/sequence** and cashier counter in header
- Pull session timeout duration from server config (instead of fixed 30 min)
- Add accessibility-only announcements (e.g., announce time remaining every 60s)
- Add quick “Search/Lookup” input box in header (Finacle has global search)
