<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-lock"></i> Account Liens</h3>
    <a href="${pageContext.request.contextPath}/accounts/${accountId}" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Account</a>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0">Active Liens</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty liens}">
                <table class="table table-hover">
                    <thead class="table-light"><tr><th>ID</th><th>Amount</th><th>Type</th><th>Start</th><th>End</th><th>Status</th><th>Approval</th><th>Actions</th></tr></thead>
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
                                    <c:when test="${l.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                    <c:when test="${l.status == 'RELEASED'}"><span class="badge bg-secondary">RELEASED</span></c:when>
                                    <c:otherwise><span class="badge bg-warning"><c:out value="${l.status}"/></span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><span class="badge bg-info"><c:out value="${l.approvalStatus}"/></span></td>
                            <td>
                                <c:if test="${l.status == 'ACTIVE'}">
                                    <form method="post" action="${pageContext.request.contextPath}/liens/${l.id}/release" style="display:inline;">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                        <button type="submit" class="btn btn-sm btn-outline-warning" onclick="return confirm('Release this lien?')"><i class="bi bi-unlock"></i> Release</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise><p class="text-muted text-center">No liens on this account.</p></c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card shadow">
    <div class="card-header bg-white"><h5 class="mb-0">Create New Lien</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/liens/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="accountId" value="${accountId}" />
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Lien Amount *</label>
                    <input type="number" name="lienAmount" class="form-control" required step="0.01" min="0.01"/>
                    <small class="text-muted">Cannot exceed ledger balance</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Lien Type *</label>
                    <select name="lienType" class="form-select" required>
                        <option value="COURT_ORDER">COURT_ORDER</option>
                        <option value="LOAN_SECURITY">LOAN_SECURITY</option>
                        <option value="TAX_RECOVERY">TAX_RECOVERY</option>
                        <option value="OTHER">OTHER</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">End Date</label>
                    <input type="date" name="endDate" class="form-control"/>
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-lock"></i> Create Lien (Pending Approval)</button>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
