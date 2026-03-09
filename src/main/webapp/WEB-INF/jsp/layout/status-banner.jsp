<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Operational Status Banner Fragment
  Displays system-wide operational alerts:
  - Holiday banner (red)
  - Customer/Account Freeze banner (amber)
  - Approval Pending banner (blue)
  - Business Day Closed banner (dark)

  Usage: <%@ include file="../layout/status-banner.jsp" %>

  Expected model/session attributes:
    sessionScope.isHoliday        - boolean, true if today is a holiday
    sessionScope.holidayName      - String, name of the holiday
    isHoliday                     - page-level holiday flag (from controller)
    freezeActive                  - boolean, entity-level freeze active
    freezeLevel                   - String, freeze level (DEBIT_ONLY, CREDIT_ONLY, FULL)
    freezeReason                  - String, reason for freeze
    approvalPending               - boolean, entity pending approval
    approvalPendingMessage        - String, custom approval message
--%>

<%-- Holiday System-Wide Red Banner --%>
<c:if test="${sessionScope.isHoliday == true || isHoliday == true}">
<div class="alert alert-danger border-0 rounded-0 mb-0 cbs-status-banner cbs-status-banner-holiday" role="alert">
    <div class="d-flex align-items-center justify-content-center">
        <i class="bi bi-calendar-x-fill me-2 fs-5"></i>
        <strong>HOLIDAY</strong>
        <span class="mx-2">|</span>
        <span>
            <c:choose>
                <c:when test="${not empty sessionScope.holidayName}"><c:out value="${sessionScope.holidayName}"/></c:when>
                <c:otherwise>Today is a bank holiday. Financial transactions are restricted.</c:otherwise>
            </c:choose>
        </span>
    </div>
</div>
</c:if>

<%-- Entity Freeze Amber Banner --%>
<c:if test="${freezeActive == true || (not empty freezeLevel && freezeLevel != 'NONE')}">
<div class="alert alert-warning border-0 rounded-0 mb-0 cbs-status-banner cbs-status-banner-freeze" role="alert">
    <div class="d-flex align-items-center justify-content-center">
        <i class="bi bi-snow me-2 fs-5"></i>
        <strong>FREEZE ACTIVE</strong>
        <span class="mx-2">|</span>
        <span>Freeze Level: <strong><c:out value="${freezeLevel}"/></strong></span>
        <c:if test="${not empty freezeReason}">
            <span class="mx-2">|</span>
            <span>Reason: <c:out value="${freezeReason}"/></span>
        </c:if>
    </div>
</div>
</c:if>

<%-- Approval Pending Blue Banner --%>
<c:if test="${approvalPending == true}">
<div class="alert alert-info border-0 rounded-0 mb-0 cbs-status-banner cbs-status-banner-approval" role="alert">
    <div class="d-flex align-items-center justify-content-center">
        <i class="bi bi-hourglass-split me-2 fs-5"></i>
        <strong>PENDING APPROVAL</strong>
        <span class="mx-2">|</span>
        <span>
            <c:choose>
                <c:when test="${not empty approvalPendingMessage}"><c:out value="${approvalPendingMessage}"/></c:when>
                <c:otherwise>This entity is pending maker-checker approval.</c:otherwise>
            </c:choose>
        </span>
    </div>
</div>
</c:if>
