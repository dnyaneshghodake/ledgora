<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-piggy-bank"></i> Deposit Portfolio Dashboard</h3>
    <a href="${pageContext.request.contextPath}/deposit/list" class="btn btn-sm btn-outline-primary"><i class="bi bi-list-ul"></i> Full Portfolio</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Portfolio Summary --%>
<div class="row g-3 mb-4">
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-primary">
            <div class="card-body text-center">
                <small class="text-muted d-block">Total Portfolio</small>
                <h4 class="mb-0"><fmt:formatNumber value="${totalPortfolio}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-success">
            <div class="card-body text-center">
                <small class="text-muted d-block">CASA Balance</small>
                <h4 class="mb-0"><fmt:formatNumber value="${casaBalance}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-info">
            <div class="card-body text-center">
                <small class="text-muted d-block">FD Portfolio</small>
                <h4 class="mb-0"><fmt:formatNumber value="${fdBalance}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-warning">
            <div class="card-body text-center">
                <small class="text-muted d-block">RD Portfolio</small>
                <h4 class="mb-0"><fmt:formatNumber value="${rdBalance}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-danger">
            <div class="card-body text-center">
                <small class="text-muted d-block">Interest Payable</small>
                <h4 class="mb-0 text-danger"><fmt:formatNumber value="${interestPayable}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow border-start border-4 border-secondary">
            <div class="card-body text-center">
                <small class="text-muted d-block">Maturing (30d)</small>
                <h4 class="mb-0"><c:out value="${maturingCount}"/></h4>
            </div>
        </div>
    </div>
</div>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Deposit portfolio data are <strong>derived caches</strong> — the source of truth is the immutable ledger entries created by the voucher engine.
    Interest accrual runs daily at EOD (DR Interest Expense GL, CR Deposit Liability GL). Premature closure posted via voucher engine.
    CASA quarterly posting per RBI. FD/RD interest at maturity. DICGC coverage: up to &#8377;5,00,000 per depositor.
    <br/><i class="bi bi-journal-text"></i> View individual deposit detail pages for the complete ledger entry audit trail.
</div>

<%@ include file="../layout/footer.jsp" %>
