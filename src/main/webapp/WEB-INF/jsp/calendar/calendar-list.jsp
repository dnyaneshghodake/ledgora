<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-calendar3"></i> Banking Calendar</h3>
    <a href="${pageContext.request.contextPath}/calendar/create" class="btn btn-primary">
        <i class="bi bi-plus-circle"></i> Add Calendar Entry
    </a>
</div>

<c:if test="${not empty success}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${success}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<!-- Pending Approvals -->
<c:if test="${not empty pendingEntries}">
<div class="card shadow mb-4">
    <div class="card-header bg-warning text-dark"><h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Pending Approval</h5></div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Date</th><th>Day Type</th><th>Holiday Name</th><th>ATM</th><th>System Txn</th><th>Actions</th></tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${pendingEntries}">
                    <tr>
                        <td><c:out value="${entry.calendarDate}"/></td>
                        <td><span class="badge ${entry.dayType == 'HOLIDAY' ? 'bg-danger' : 'bg-success'}"><c:out value="${entry.dayType}"/></span></td>
                        <td><c:out value="${entry.holidayName}"/></td>
                        <td>${entry.atmAllowed ? 'Yes' : 'No'}</td>
                        <td>${entry.systemTransactionsAllowed ? 'Yes' : 'No'}</td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/calendar/approve/${entry.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-success"><i class="bi bi-check-circle"></i> Approve</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/calendar/reject/${entry.id}" style="display:inline;">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <input type="hidden" name="reason" value="Rejected by checker"/>
                                <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-x-circle"></i> Reject</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
</c:if>

<!-- Upcoming Holidays -->
<c:if test="${not empty upcomingHolidays}">
<div class="card shadow mb-4">
    <div class="card-header bg-info text-white"><h5 class="mb-0"><i class="bi bi-calendar-event"></i> Upcoming Holidays</h5></div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Date</th><th>Holiday Name</th><th>Type</th><th>ATM Allowed</th><th>System Txn Allowed</th></tr>
            </thead>
            <tbody>
                <c:forEach var="h" items="${upcomingHolidays}">
                    <tr>
                        <td><c:out value="${h.calendarDate}"/></td>
                        <td><c:out value="${h.holidayName}"/></td>
                        <td><c:out value="${h.holidayType}"/></td>
                        <td>${h.atmAllowed ? 'Yes' : 'No'}</td>
                        <td>${h.systemTransactionsAllowed ? 'Yes' : 'No'}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
</c:if>

<!-- All Calendar Entries -->
<div class="card shadow">
    <div class="card-header"><h5 class="mb-0">All Calendar Entries</h5></div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Date</th><th>Day Type</th><th>Holiday Name</th><th>Status</th><th>ATM</th><th>System Txn</th></tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${calendarEntries}">
                    <tr>
                        <td><c:out value="${entry.calendarDate}"/></td>
                        <td><span class="badge ${entry.dayType == 'HOLIDAY' ? 'bg-danger' : 'bg-success'}"><c:out value="${entry.dayType}"/></span></td>
                        <td><c:out value="${entry.holidayName}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${entry.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                <c:when test="${entry.approvalStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                <c:when test="${entry.approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                            </c:choose>
                        </td>
                        <td>${entry.atmAllowed ? 'Yes' : 'No'}</td>
                        <td>${entry.systemTransactionsAllowed ? 'Yes' : 'No'}</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty calendarEntries}">
                    <tr><td colspan="6" class="text-center text-muted py-4">No calendar entries found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
