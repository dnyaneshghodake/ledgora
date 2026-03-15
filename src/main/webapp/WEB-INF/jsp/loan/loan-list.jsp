<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-list-ul"></i> Loan Portfolio</h3>
    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Filters --%>
<div class="card shadow mb-3">
    <div class="card-body py-2">
        <form method="get" class="row g-2 align-items-end">
            <div class="col-md-3">
                <label class="form-label form-label-sm">Status</label>
                <select name="status" class="form-select form-select-sm">
                    <option value="">All</option>
                    <option value="ACTIVE" ${selectedStatus == 'ACTIVE' ? 'selected' : ''}>Active</option>
                    <option value="NPA" ${selectedStatus == 'NPA' ? 'selected' : ''}>NPA</option>
                    <option value="WRITTEN_OFF" ${selectedStatus == 'WRITTEN_OFF' ? 'selected' : ''}>Written Off</option>
                    <option value="CLOSED" ${selectedStatus == 'CLOSED' ? 'selected' : ''}>Closed</option>
                </select>
            </div>
            <div class="col-md-3">
                <label class="form-label form-label-sm">NPA Category</label>
                <select name="npaCategory" class="form-select form-select-sm">
                    <option value="">All</option>
                    <option value="STANDARD" ${selectedNpaCategory == 'STANDARD' ? 'selected' : ''}>Standard</option>
                    <option value="SUBSTANDARD" ${selectedNpaCategory == 'SUBSTANDARD' ? 'selected' : ''}>Substandard</option>
                    <option value="DOUBTFUL" ${selectedNpaCategory == 'DOUBTFUL' ? 'selected' : ''}>Doubtful</option>
                    <option value="LOSS" ${selectedNpaCategory == 'LOSS' ? 'selected' : ''}>Loss</option>
                </select>
            </div>
            <div class="col-md-2"><button type="submit" class="btn btn-sm btn-primary"><i class="bi bi-funnel"></i> Filter</button></div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover reg-table">
                <thead class="table-light">
                    <tr><th>Loan #</th><th>Borrower</th><th class="text-end">Principal</th><th class="text-end">Outstanding</th><th class="text-end">EMI</th><th>DPD</th><th>Status</th><th>NPA Category</th><th></th></tr>
                </thead>
                <tbody>
                    <c:forEach var="loan" items="${loans}">
                    <tr>
                        <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                        <td><c:out value="${loan.borrowerName}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${loan.principalAmount}" maxFractionDigits="0"/></td>
                        <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                        <td class="text-end"><c:if test="${loan.emiAmount != null}"><fmt:formatNumber value="${loan.emiAmount}" maxFractionDigits="0"/></c:if></td>
                        <td class="${loan.dpd > 90 ? 'text-danger fw-bold' : loan.dpd > 0 ? 'text-warning' : ''}"><c:out value="${loan.dpd}"/></td>
                        <td><c:choose>
                            <c:when test="${loan.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                            <c:when test="${loan.status == 'NPA'}"><span class="badge bg-danger">NPA</span></c:when>
                            <c:when test="${loan.status == 'CLOSED'}"><span class="badge bg-secondary">CLOSED</span></c:when>
                            <c:otherwise><span class="badge bg-dark"><c:out value="${loan.status}"/></span></c:otherwise>
                        </c:choose></td>
                        <td><c:choose>
                            <c:when test="${loan.npaClassification == 'STANDARD'}"><span class="badge bg-success">Standard</span></c:when>
                            <c:when test="${loan.npaClassification == 'SUBSTANDARD'}"><span class="badge bg-warning text-dark">Substandard</span></c:when>
                            <c:when test="${loan.npaClassification == 'DOUBTFUL'}"><span class="badge bg-danger">Doubtful</span></c:when>
                            <c:when test="${loan.npaClassification == 'LOSS'}"><span class="badge" style="background-color:#8b0000;color:#fff;">Loss</span></c:when>
                        </c:choose></td>
                        <td><a href="${pageContext.request.contextPath}/loan/${loan.id}" class="btn btn-sm btn-outline-primary">View</a></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
        <c:if test="${empty loans}">
            <div class="text-center py-4 text-muted"><i class="bi bi-inbox" style="font-size:2rem;"></i><p>No loans found matching filters.</p></div>
        </c:if>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
