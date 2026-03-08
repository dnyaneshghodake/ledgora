<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-calendar3"></i> Banking Calendar</h3>
    <a href="${pageContext.request.contextPath}/calendar/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> Add Entry</a>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty entries}">
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light"><tr><th>Date</th><th>Type</th><th>Description</th><th>Status</th></tr></thead>
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
                                <td><span class="badge bg-info"><c:out value="${e.approvalStatus}"/></span></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise><div class="text-center py-4 text-muted"><p>No calendar entries.</p></div></c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
