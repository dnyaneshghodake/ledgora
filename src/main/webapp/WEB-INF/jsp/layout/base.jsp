<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Ledgora CBS Base Layout Template — Finacle-Grade Enterprise Banking UI

  All authenticated pages use the header.jsp / footer.jsp include pattern
  which provides the following CBS layout:

  +-------------------------------------------------------------------+
  |  CBS HEADER (fixed top, role="banner")                            |
  |  [40px Logo] LEDGORA | Branch | Env | BizDate | Clock |          |
  |  Status | Health | Tenant | Bell | Avatar+Role | Logout           |
  +---------------+---------------------------------------------------+
  |  SIDEBAR      |  MAIN CONTENT AREA (role="main")                  |
  |  (fixed left) |                                                   |
  |  role="nav"   |  Breadcrumbs (optional)                           |
  |               |  Alerts (success/error, auto-dismiss)             |
  |  Dashboard    |                                                   |
  |  Customers    |  Page-specific content                            |
  |  Accounts     |  (injected via include pattern)                   |
  |  Transactions |  Cards, tables, forms — all enterprise-themed     |
  |  Payments     |                                                   |
  |  Ledger       |  max-width: 1600px, uniform 1.25rem padding       |
  |  Settlement   |                                                   |
  |  Approvals    |                                                   |
  |  Admin        |                                                   |
  +---------------+---------------------------------------------------+
  |  FOOTER (role="contentinfo")                                      |
  |  CBS v2.7 | Disclaimer | Copyright | Powered by Spring Boot       |
  +-------------------------------------------------------------------+

  Usage in content pages:
    <%@ include file="../layout/header.jsp" %>
    ... page content ...
    <%@ include file="../layout/footer.jsp" %>

  Layout Files:
    header.jsp  - Opens HTML, renders header bar (dark #002147), includes sidebar, opens main
    sidebar.jsp - Role-based collapsible sidebar navigation (included by header.jsp)
    footer.jsp  - Closes main area, footer with version/copyright, scripts, closes HTML
    base.jsp    - This documentation file (no rendered output)

  Theme CSS (load order matters — each overrides the previous):
    1. bootstrap.min.css       — Base Bootstrap 5 framework
    2. bootstrap-icons.css     — Icon font
    3. style.css               — Ledgora CBS structural layout + component styles
    4. enterprise-theme.css    — Finacle-style overrides: palette, typography, density, sidebar

  ARIA Landmarks:
    header  → role="banner"
    aside   → role="navigation" aria-label="Main sidebar navigation"
    main    → role="main"
    footer  → role="contentinfo"
    nav     → aria-label="breadcrumb"

  Responsive Behavior (<1024px):
    - Sidebar collapses to 72px mini-icons (text labels hidden)
    - Header center section hides (branch, date, clock, status)
    - Brand text hides; logo + hamburger remain visible

  Standalone Pages (no sidebar layout):
    - /auth/login.jsp
    - /auth/register.jsp
--%>
