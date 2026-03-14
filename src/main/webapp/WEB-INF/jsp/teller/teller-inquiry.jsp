<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-search"></i> Teller Inquiry</h3>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show" role="alert">
        <i class="bi bi-check-circle-fill me-2"></i><c:out value="${message}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${error}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<div class="row g-3 mb-4">
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-header bg-primary text-white"><h5 class="mb-0"><i class="bi bi-person-workspace me-2"></i>Quick Actions</h5></div>
            <div class="card-body">
                <div class="d-grid gap-2">
                    <a href="${pageContext.request.contextPath}/teller/open" class="btn btn-outline-primary"><i class="bi bi-box-arrow-in-right me-2"></i>Open Teller Session</a>
                    <a href="${pageContext.request.contextPath}/teller/deposit" class="btn btn-outline-success"><i class="bi bi-arrow-down-circle me-2"></i>Cash Deposit</a>
                    <a href="${pageContext.request.contextPath}/teller/withdraw" class="btn btn-outline-danger"><i class="bi bi-arrow-up-circle me-2"></i>Cash Withdrawal</a>
                    <a href="${pageContext.request.contextPath}/teller/vault-transfer" class="btn btn-outline-warning"><i class="bi bi-safe me-2"></i>Vault Transfer</a>
                    <a href="${pageContext.request.contextPath}/teller/close" class="btn btn-outline-secondary"><i class="bi bi-box-arrow-right me-2"></i>Close Teller Session</a>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-header bg-info text-white"><h5 class="mb-0"><i class="bi bi-info-circle me-2"></i>Teller Module Status</h5></div>
            <div class="card-body">
                <p class="mb-2"><strong>Teller State Machine:</strong></p>
                <div class="mb-3">
                    <span class="badge bg-secondary">ASSIGNED</span>
                    <i class="bi bi-arrow-right mx-1"></i>
                    <span class="badge bg-warning text-dark">OPEN_REQUESTED</span>
                    <i class="bi bi-arrow-right mx-1"></i>
                    <span class="badge bg-success">OPEN</span>
                    <i class="bi bi-arrow-right mx-1"></i>
                    <span class="badge bg-warning text-dark">CLOSING_REQUESTED</span>
                    <i class="bi bi-arrow-right mx-1"></i>
                    <span class="badge bg-dark">CLOSED</span>
                </div>
                <p class="text-muted mb-1"><i class="bi bi-shield-check me-1"></i> Maker-checker enforced on open/close</p>
                <p class="text-muted mb-1"><i class="bi bi-lock me-1"></i> Dual custody required for vault transfers</p>
                <p class="text-muted mb-1"><i class="bi bi-cash-coin me-1"></i> Denomination capture mandatory</p>
                <p class="text-muted mb-0"><i class="bi bi-calculator me-1"></i> EOD reconciliation enforced at closure</p>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
