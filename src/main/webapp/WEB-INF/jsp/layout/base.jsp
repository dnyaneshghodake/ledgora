<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Ledgora CBS Base Layout Template

  All authenticated pages use the header.jsp / footer.jsp include pattern
  which provides the following CBS layout:

  +-------------------------------------------------------------------+
  |  CBS HEADER (fixed top)                                           |
  |  Logo | Business Date | User Info | Role Badge | Logout           |
  +---------------+---------------------------------------------------+
  |  SIDEBAR      |  MAIN CONTENT AREA                                |
  |  (fixed left) |                                                   |
  |               |  Alerts (success/error)                           |
  |  Dashboard    |                                                   |
  |  Customers    |  Page-specific content                            |
  |  Accounts     |  (injected via include pattern)                   |
  |  Transactions |                                                   |
  |  Payments     |                                                   |
  |  Ledger       |                                                   |
  |  Settlement   |                                                   |
  |  Approvals    |                                                   |
  |  Admin        |                                                   |
  +---------------+---------------------------------------------------+
  |  FOOTER                                                           |
  +-------------------------------------------------------------------+

  Usage in content pages:
    <%@ include file="../layout/header.jsp" %>
    ... page content ...
    <%@ include file="../layout/footer.jsp" %>

  Layout Files:
    header.jsp  - Opens HTML, renders header bar, includes sidebar, opens main
    sidebar.jsp - Role-based sidebar navigation (included by header.jsp)
    footer.jsp  - Closes main area, footer, scripts, closes HTML
    base.jsp    - This documentation file

  Standalone Pages (no sidebar layout):
    - /auth/login.jsp
    - /auth/register.jsp
--%>
