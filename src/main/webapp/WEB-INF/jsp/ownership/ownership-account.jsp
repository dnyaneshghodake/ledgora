<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> Account Owners</h3>
    <a href="${pageContext.request.contextPath}/ownership" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back
    </a>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Customer</th><th>Type</th><th>Percentage</th><th>Operational</th><th>Status</th></tr>
            </thead>
            <tbody>
                <c:forEach var="o" items="${ownerships}">
                    <tr>
                        <td><c:out value="${o.customerMaster.customerNumber}"/> - <c:out value="${o.customerMaster.firstName}"/> <c:out value="${o.customerMaster.lastName}"/></td>
                        <td><span class="badge bg-info"><c:out value="${o.ownershipType}"/></span></td>
                        <td><c:out value="${o.ownershipPercentage}"/>%</td>
                        <td>${o.isOperational ? 'Yes' : 'No'}</td>
                        <td><span class="badge bg-success"><c:out value="${o.approvalStatus}"/></span></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty ownerships}">
                    <tr><td colspan="5" class="text-center text-muted py-4">No ownership records found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
