<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-lock"></i> Account Liens</h3>
    <a href="${pageContext.request.contextPath}/liens" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back
    </a>
</div>

<div class="alert alert-info">
    <strong>Total Active Lien Amount:</strong> <c:out value="${totalLienAmount}"/>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>ID</th><th>Amount</th><th>Type</th><th>Start</th><th>End</th><th>Status</th><th>Approval</th><th>Actions</th></tr>
            </thead>
            <tbody>
                <c:forEach var="l" items="${liens}">
                    <tr>
                        <td><c:out value="${l.id}"/></td>
                        <td class="fw-bold"><c:out value="${l.lienAmount}"/></td>
                        <td><span class="badge bg-info"><c:out value="${l.lienType}"/></span></td>
                        <td><c:out value="${l.startDate}"/></td>
                        <td><c:out value="${l.endDate}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${l.status == 'ACTIVE'}"><span class="badge bg-danger">ACTIVE</span></c:when>
                                <c:when test="${l.status == 'RELEASED'}"><span class="badge bg-success">RELEASED</span></c:when>
                                <c:when test="${l.status == 'EXPIRED'}"><span class="badge bg-secondary">EXPIRED</span></c:when>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${l.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                <c:when test="${l.approvalStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                <c:when test="${l.approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${l.status == 'ACTIVE' && l.approvalStatus == 'APPROVED'}">
                                <form method="post" action="${pageContext.request.contextPath}/liens/release/${l.id}" style="display:inline;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <button type="submit" class="btn btn-sm btn-outline-danger"><i class="bi bi-unlock"></i> Release</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty liens}">
                    <tr><td colspan="8" class="text-center text-muted py-4">No liens found for this account</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
