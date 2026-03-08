<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-calendar3"></i> Banking Calendar</h3>
    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
    <a href="${pageContext.request.contextPath}/calendar/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> Add Entry</a>
    </c:if>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty entries}">
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light"><tr><th>Date</th><th>Type</th><th>Description</th><th>Created By</th><th>Created At</th><th>Approved By</th><th>Approval Status</th><th>Last Updated</th></tr></thead>
                        <tbody>
                            <c:forEach var="e" items="${entries}">
                            <tr>
                                <td><c:out value="${e.calendarDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${e.dayType == 'HOLIDAY'}"><span class="badge bg-danger">HOLIDAY</span></c:when>
                                        <c:otherwise><span class="badge bg-success">WORKING_DAY</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${e.description}"/></td>
                                <td><c:out value="${e.createdBy != null ? e.createdBy.username : 'System'}"/></td>
                                <td><small><c:out value="${e.createdAt}"/></small></td>
                                <td><c:out value="${e.approvedBy != null ? e.approvedBy.username : '--'}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${e.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                        <c:when test="${e.approvalStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                        <c:when test="${e.approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                        <c:otherwise><span class="badge bg-info"><c:out value="${e.approvalStatus}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><small><c:out value="${e.updatedAt}"/></small></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-calendar3" style="font-size: 3rem;"></i>
                    <p class="mt-2">No calendar entries.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
