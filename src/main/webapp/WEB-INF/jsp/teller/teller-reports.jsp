<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-file-earmark-bar-graph"></i> Teller Cash Reports</h3>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Teller Inquiry</a>
</div>

<div class="row g-3">
    <div class="col-md-4">
        <div class="card shadow-sm h-100">
            <div class="card-body text-center">
                <i class="bi bi-person-workspace fs-1 text-primary"></i>
                <h5 class="mt-2">Teller Cash Position</h5>
                <p class="text-muted">Per-teller opening, current balance, credits/debits for a business date.</p>
                <a href="${pageContext.request.contextPath}/teller/reports/cash-position" class="btn btn-primary">View Report</a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm h-100">
            <div class="card-body text-center">
                <i class="bi bi-safe fs-1 text-warning"></i>
                <h5 class="mt-2">Vault Position</h5>
                <p class="text-muted">Current vault balance, holding limit, and utilization per branch.</p>
                <a href="${pageContext.request.contextPath}/teller/reports/vault-position" class="btn btn-warning">View Report</a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm h-100">
            <div class="card-body text-center">
                <i class="bi bi-arrow-left-right fs-1 text-info"></i>
                <h5 class="mt-2">Daily Cash Movement</h5>
                <p class="text-muted">Aggregated credits, debits, net movement for all tellers on a date.</p>
                <a href="${pageContext.request.contextPath}/teller/reports/daily-movement" class="btn btn-info">View Report</a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm h-100">
            <div class="card-body text-center">
                <i class="bi bi-cash-coin fs-1 text-success"></i>
                <h5 class="mt-2">Denomination Summary</h5>
                <p class="text-muted">Aggregated note count per denomination for a teller session.</p>
                <a href="${pageContext.request.contextPath}/teller/reports/cash-position" class="btn btn-success">Select Session First</a>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow-sm h-100">
            <div class="card-body text-center">
                <i class="bi bi-exclamation-triangle fs-1 text-danger"></i>
                <h5 class="mt-2">Cash Short / Excess</h5>
                <p class="text-muted">Reconciliation mismatches detected during teller closure.</p>
                <a href="${pageContext.request.contextPath}/teller/reports/short-excess" class="btn btn-danger">View Report</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
