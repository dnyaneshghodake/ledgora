<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-circle"></i> Customer 360&deg; View</h3>
    <div>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Back to List</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- STICKY CIF SUMMARY PANEL (Finacle-style)                          --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="card border-primary mb-3 shadow cbs-cif-panel" style="position:sticky; top:56px; z-index:100;">
    <div class="card-body py-2">
        <div class="row align-items-center">
            <div class="col-md-3">
                <div class="d-flex align-items-center">
                    <i class="bi bi-person-circle fs-2 text-primary me-2"></i>
                    <div>
                        <h5 class="mb-0"><c:out value="${c360.fullName}"/></h5>
                        <small class="text-muted">CIF: <code><c:out value="${c360.customerId}"/></code></small>
                    </div>
                </div>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Type</small>
                <strong><c:out value="${c360.customerType}" default="--"/></strong>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Risk Category</small>
                <c:choose>
                    <c:when test="${c360.riskCategory == 'HIGH'}"><span class="badge bg-danger"><c:out value="${c360.riskCategory}"/></span></c:when>
                    <c:when test="${c360.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark"><c:out value="${c360.riskCategory}"/></span></c:when>
                    <c:otherwise><span class="badge bg-success"><c:out value="${c360.riskCategory}" default="LOW"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">KYC Status</small>
                <c:choose>
                    <c:when test="${c360.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                    <c:when test="${c360.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                    <c:when test="${c360.kycStatus == 'EXPIRED'}"><span class="badge bg-danger">EXPIRED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${c360.kycStatus}" default="N/A"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Approval</small>
                <c:choose>
                    <c:when test="${c360.approvalStatus == 'APPROVED'}"><span class="badge cbs-badge-approved">APPROVED</span></c:when>
                    <c:when test="${c360.approvalStatus == 'PENDING'}"><span class="badge cbs-badge-pending">PENDING</span></c:when>
                    <c:when test="${c360.approvalStatus == 'REJECTED'}"><span class="badge cbs-badge-rejected">REJECTED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${c360.approvalStatus}" default="--"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-1 text-end">
                <c:if test="${c360.freezeLevel != null && c360.freezeLevel != 'NONE'}">
                    <span class="badge bg-danger" title="Freeze: ${c360.freezeLevel}"><i class="bi bi-snow"></i> <c:out value="${c360.freezeLevel}"/></span>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%-- Auditor Read-Only Banner --%>
<c:if test="${isAuditor}">
<div class="card border-info mb-3 shadow-sm">
    <div class="card-header bg-info text-white d-flex align-items-center py-2">
        <i class="bi bi-eye fs-5 me-2"></i>
        <strong>Audit Review (Read-Only)</strong>
    </div>
</div>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- KPI CARDS                                                          --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="row mb-4 g-2">
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Total Accounts</div>
                <div class="fs-3 fw-bold text-primary"><c:out value="${c360.totalAccounts}"/></div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Ledger Balance</div>
                <div class="fs-4 fw-bold text-success"><fmt:formatNumber value="${c360.totalLedgerBalance}" type="number" minFractionDigits="2" maxFractionDigits="2"/></div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Available Balance</div>
                <div class="fs-4 fw-bold text-info"><fmt:formatNumber value="${c360.totalAvailableBalance}" type="number" minFractionDigits="2" maxFractionDigits="2"/></div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Total Liens</div>
                <div class="fs-4 fw-bold text-warning"><fmt:formatNumber value="${c360.totalLienAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Open Suspense</div>
                <div class="fs-4 fw-bold ${c360.openSuspenseCount > 0 ? 'text-danger' : 'text-muted'}"><c:out value="${c360.openSuspenseCount}"/>
                    <small class="fs-6">(<fmt:formatNumber value="${c360.openSuspenseAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/>)</small>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Open IBT</div>
                <div class="fs-4 fw-bold ${c360.openIbtCount > 0 ? 'text-warning' : 'text-muted'}"><c:out value="${c360.openIbtCount}"/>
                    <small class="fs-6">(<fmt:formatNumber value="${c360.openIbtAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/>)</small>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Fraud Alerts</div>
                <div class="fs-4 fw-bold ${c360.openFraudAlertCount > 0 ? 'text-danger' : 'text-muted'}"><c:out value="${c360.openFraudAlertCount}"/></div>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6">
        <div class="card shadow-sm border-0 h-100">
            <div class="card-body text-center py-3">
                <div class="text-muted small">Under Review</div>
                <div class="fs-4 fw-bold ${c360.accountsUnderReviewCount > 0 ? 'text-warning' : 'text-muted'}"><c:out value="${c360.accountsUnderReviewCount}"/></div>
            </div>
        </div>
    </div>
</div>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- TAB NAVIGATION                                                    --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<ul class="nav nav-tabs cbs-360-tabs mb-0" id="c360Tabs" role="tablist">
    <li class="nav-item" role="presentation">
        <button class="nav-link active" id="tab-overview" data-bs-toggle="tab" data-bs-target="#pane-overview" type="button" role="tab">
            <i class="bi bi-info-circle"></i> Overview
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-accounts" data-bs-toggle="tab" data-bs-target="#pane-accounts" type="button" role="tab">
            <i class="bi bi-wallet2"></i> Accounts
            <span class="badge bg-secondary ms-1"><c:out value="${c360.totalAccounts}"/></span>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-transactions" data-bs-toggle="tab" data-bs-target="#pane-transactions" type="button" role="tab">
            <i class="bi bi-arrow-left-right"></i> Transactions
            <span class="badge bg-secondary ms-1"><c:out value="${c360.totalTransactionCount}"/></span>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-ibt" data-bs-toggle="tab" data-bs-target="#pane-ibt" type="button" role="tab">
            <i class="bi bi-building"></i> IBT Exposure
            <c:if test="${c360.unsettledIbtCount > 0}"><span class="badge bg-warning text-dark ms-1"><c:out value="${c360.unsettledIbtCount}"/></span></c:if>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-suspense" data-bs-toggle="tab" data-bs-target="#pane-suspense" type="button" role="tab">
            <i class="bi bi-exclamation-triangle"></i> Suspense
            <c:if test="${c360.openSuspenseCount > 0}"><span class="badge bg-danger ms-1"><c:out value="${c360.openSuspenseCount}"/></span></c:if>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-risk" data-bs-toggle="tab" data-bs-target="#pane-risk" type="button" role="tab">
            <i class="bi bi-shield-exclamation"></i> Risk &amp; Governance
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-audit" data-bs-toggle="tab" data-bs-target="#pane-audit" type="button" role="tab">
            <i class="bi bi-clock-history"></i> Audit Trail
            <span class="badge bg-secondary ms-1"><c:out value="${fn:length(c360.auditTrail)}"/></span>
        </button>
    </li>
</ul>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- TAB CONTENT                                                       --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="tab-content cbs-360-tab-content" id="c360TabContent">

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 1: OVERVIEW                                                    --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade show active" id="pane-overview" role="tabpanel">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-person-vcard me-2"></i>Customer Details</h5>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">CIF / Customer ID</td><td><code class="fs-5"><c:out value="${c360.customerId}"/></code></td></tr>
                        <tr><td class="cbs-label">Full Name</td><td><strong><c:out value="${c360.fullName}"/></strong></td></tr>
                        <tr><td class="cbs-label">Customer Type</td><td><c:out value="${c360.customerType}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Risk Category</td><td>
                            <c:choose>
                                <c:when test="${c360.riskCategory == 'HIGH'}"><span class="badge bg-danger">HIGH</span></c:when>
                                <c:when test="${c360.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark">MEDIUM</span></c:when>
                                <c:otherwise><span class="badge bg-success"><c:out value="${c360.riskCategory}" default="LOW"/></span></c:otherwise>
                            </c:choose>
                        </td></tr>
                        <tr><td class="cbs-label">KYC Status</td><td>
                            <c:choose>
                                <c:when test="${c360.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                                <c:when test="${c360.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                                <c:when test="${c360.kycStatus == 'EXPIRED'}"><span class="badge bg-danger">EXPIRED</span></c:when>
                                <c:otherwise><span class="badge bg-secondary"><c:out value="${c360.kycStatus}" default="N/A"/></span></c:otherwise>
                            </c:choose>
                        </td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">National ID</td><td><c:out value="${c360.nationalId}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Email</td><td><c:out value="${c360.email}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Phone</td><td><c:out value="${c360.phone}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Address</td><td><c:out value="${c360.address}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Freeze Level</td><td>
                            <c:choose>
                                <c:when test="${c360.freezeLevel == 'NONE' || c360.freezeLevel == null}"><span class="text-muted">NONE</span></c:when>
                                <c:when test="${c360.freezeLevel == 'FULL'}"><span class="badge bg-danger">FULL</span></c:when>
                                <c:otherwise><span class="badge bg-warning text-dark"><c:out value="${c360.freezeLevel}"/></span></c:otherwise>
                            </c:choose>
                        </td></tr>
                        <tr><td class="cbs-label">Created</td><td><c:out value="${c360.createdAt}"/></td></tr>
                        <tr><td class="cbs-label">Last Updated</td><td><c:out value="${c360.updatedAt}"/></td></tr>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 2: ACCOUNTS                                                    --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-accounts" role="tabpanel">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-wallet2 me-2"></i>Accounts (<c:out value="${c360.totalAccounts}"/>)</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.accounts}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th></th>
                                <th>Account Number</th>
                                <th>Branch</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th class="text-end">Ledger Balance</th>
                                <th class="text-end">Available</th>
                                <th class="text-end">Lien</th>
                                <th>Freeze</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="acct" items="${c360.accounts}" varStatus="vs">
                            <tr class="cbs-voucher-row" data-bs-toggle="collapse" data-bs-target="#acctDetail${vs.index}" style="cursor:pointer;">
                                <td><i class="bi bi-chevron-right cbs-expand-icon"></i></td>
                                <td><code><c:out value="${acct.accountNumber}"/></code></td>
                                <td><c:out value="${acct.branchCode}" default="--"/></td>
                                <td><c:out value="${acct.accountType}" default="--"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${acct.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                        <c:when test="${acct.status == 'FROZEN'}"><span class="badge bg-danger">FROZEN</span></c:when>
                                        <c:when test="${acct.status == 'UNDER_REVIEW'}"><span class="badge bg-warning text-dark">UNDER REVIEW</span></c:when>
                                        <c:when test="${acct.status == 'CLOSED'}"><span class="badge bg-secondary">CLOSED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${acct.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${acct.ledgerBalance}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td class="text-end"><fmt:formatNumber value="${acct.availableBalance}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td class="text-end"><fmt:formatNumber value="${acct.lienAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${acct.freezeLevel == 'NONE'}"><span class="text-muted">NONE</span></c:when>
                                        <c:when test="${acct.freezeLevel == 'FULL'}"><span class="badge bg-danger">FULL</span></c:when>
                                        <c:otherwise><span class="badge bg-warning text-dark"><c:out value="${acct.freezeLevel}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <%-- Expandable: Last 5 Transactions --%>
                            <tr class="collapse" id="acctDetail${vs.index}">
                                <td colspan="9" class="bg-light p-3">
                                    <h6 class="text-muted mb-2"><i class="bi bi-clock-history"></i> Recent Transactions</h6>
                                    <c:choose>
                                        <c:when test="${not empty acct.recentTransactions}">
                                        <table class="table table-sm table-bordered mb-0">
                                            <thead><tr><th>Ref</th><th>Type</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
                                            <tbody>
                                                <c:forEach var="rtxn" items="${acct.recentTransactions}">
                                                <tr>
                                                    <td><a href="${pageContext.request.contextPath}/transactions/${rtxn.transactionId}/view"><code><c:out value="${rtxn.transactionRef}"/></code></a></td>
                                                    <td><c:out value="${rtxn.transactionType}"/></td>
                                                    <td class="text-end"><fmt:formatNumber value="${rtxn.amount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                                    <td>
                                                        <c:choose>
                                                            <c:when test="${rtxn.status == 'COMPLETED'}"><span class="badge cbs-badge-completed">COMPLETED</span></c:when>
                                                            <c:when test="${rtxn.status == 'PENDING_APPROVAL'}"><span class="badge cbs-badge-pending">PENDING</span></c:when>
                                                            <c:otherwise><span class="badge bg-secondary"><c:out value="${rtxn.status}"/></span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <td><c:out value="${rtxn.createdAt}"/></td>
                                                </tr>
                                                </c:forEach>
                                            </tbody>
                                        </table>
                                        </c:when>
                                        <c:otherwise><span class="text-muted">No recent transactions.</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No accounts found for this customer.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 3: TRANSACTIONS (Paginated)                                    --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-transactions" role="tabpanel">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="bi bi-arrow-left-right me-2"></i>Transactions (<c:out value="${c360.totalTransactionCount}"/>)</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.transactions}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>Txn ID</th>
                                <th>Reference</th>
                                <th>Type</th>
                                <th>Channel</th>
                                <th class="text-end">Amount</th>
                                <th>Status</th>
                                <th>Business Date</th>
                                <th>Maker</th>
                                <th>Checker</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="txn" items="${c360.transactions}">
                            <tr style="cursor:pointer;" onclick="window.location='${pageContext.request.contextPath}/transactions/${txn.transactionId}/view'">
                                <td><code><c:out value="${txn.transactionId}"/></code></td>
                                <td><code><c:out value="${txn.transactionRef}"/></code></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${txn.transactionType == 'DEPOSIT'}"><span class="badge bg-success">DEPOSIT</span></c:when>
                                        <c:when test="${txn.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger">WITHDRAWAL</span></c:when>
                                        <c:when test="${txn.transactionType == 'TRANSFER'}"><span class="badge bg-info">TRANSFER</span></c:when>
                                        <c:when test="${txn.transactionType == 'REVERSAL'}"><span class="badge bg-secondary">REVERSAL</span></c:when>
                                        <c:otherwise><span class="badge bg-dark"><c:out value="${txn.transactionType}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${txn.channel}" default="--"/></td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${txn.amount}" type="number" minFractionDigits="2" maxFractionDigits="2"/> <small><c:out value="${txn.currency}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${txn.status == 'COMPLETED'}"><span class="badge cbs-badge-completed">COMPLETED</span></c:when>
                                        <c:when test="${txn.status == 'PENDING_APPROVAL'}"><span class="badge cbs-badge-pending">PENDING</span></c:when>
                                        <c:when test="${txn.status == 'REVERSED'}"><span class="badge cbs-badge-reversed">REVERSED</span></c:when>
                                        <c:when test="${txn.status == 'FAILED'}"><span class="badge cbs-badge-failed">FAILED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${txn.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${txn.businessDate}"/></td>
                                <td><c:out value="${txn.makerUsername}" default="--"/></td>
                                <td><c:out value="${txn.checkerUsername}" default="--"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <%-- Pagination --%>
                <c:if test="${c360.totalTransactionCount > pageSize}">
                <nav class="p-3">
                    <ul class="pagination pagination-sm justify-content-center mb-0">
                        <c:if test="${currentPage > 0}">
                            <li class="page-item"><a class="page-link" href="${pageContext.request.contextPath}/customers/${c360.customerId}/360?page=${currentPage - 1}&size=${pageSize}#pane-transactions">&laquo; Prev</a></li>
                        </c:if>
                        <li class="page-item active"><a class="page-link" href="#">Page <c:out value="${currentPage + 1}"/></a></li>
                        <c:if test="${(currentPage + 1) * pageSize < c360.totalTransactionCount}">
                            <li class="page-item"><a class="page-link" href="${pageContext.request.contextPath}/customers/${c360.customerId}/360?page=${currentPage + 1}&size=${pageSize}#pane-transactions">Next &raquo;</a></li>
                        </c:if>
                    </ul>
                </nav>
                </c:if>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No transactions found.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 4: IBT EXPOSURE                                                --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-ibt" role="tabpanel">
    <%-- IBT KPIs --%>
    <div class="row mb-3 g-2">
        <div class="col-md-4">
            <div class="card border-warning shadow-sm">
                <div class="card-body text-center py-2">
                    <div class="text-muted small">Unsettled Count</div>
                    <div class="fs-4 fw-bold text-warning"><c:out value="${c360.unsettledIbtCount}"/></div>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-info shadow-sm">
                <div class="card-body text-center py-2">
                    <div class="text-muted small">Open IBT Amount</div>
                    <div class="fs-4 fw-bold text-info"><fmt:formatNumber value="${c360.openIbtAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></div>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-primary shadow-sm">
                <div class="card-body text-center py-2">
                    <div class="text-muted small">Net Clearing Exposure</div>
                    <div class="fs-4 fw-bold ${c360.netClearingExposure != null && c360.netClearingExposure.signum() != 0 ? 'text-danger' : 'text-success'}">
                        <fmt:formatNumber value="${c360.netClearingExposure}" type="number" minFractionDigits="2" maxFractionDigits="2"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-building me-2"></i>Inter-Branch Transfers</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.ibtTransfers}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>IBT ID</th>
                                <th>From Branch</th>
                                <th>To Branch</th>
                                <th class="text-end">Amount</th>
                                <th>Status</th>
                                <th>Business Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="ibt" items="${c360.ibtTransfers}">
                            <tr>
                                <td><code><c:out value="${ibt.ibtId}"/></code></td>
                                <td><c:out value="${ibt.fromBranchName}" default="${ibt.fromBranchCode}"/></td>
                                <td><c:out value="${ibt.toBranchName}" default="${ibt.toBranchCode}"/></td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${ibt.amount}" type="number" minFractionDigits="2" maxFractionDigits="2"/> <small><c:out value="${ibt.currency}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ibt.status == 'SETTLED'}"><span class="badge bg-success">SETTLED</span></c:when>
                                        <c:when test="${ibt.status == 'INITIATED'}"><span class="badge bg-info">INITIATED</span></c:when>
                                        <c:when test="${ibt.status == 'SENT'}"><span class="badge bg-warning text-dark">SENT</span></c:when>
                                        <c:when test="${ibt.status == 'RECEIVED'}"><span class="badge bg-primary">RECEIVED</span></c:when>
                                        <c:when test="${ibt.status == 'FAILED'}"><span class="badge bg-danger">FAILED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${ibt.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${ibt.businessDate}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No inter-branch transfers found.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 5: SUSPENSE EXPOSURE                                           --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-suspense" role="tabpanel">
    <%-- Suspense Warning Banner --%>
    <c:if test="${c360.openSuspenseCount > 0}">
    <div class="alert alert-danger d-flex align-items-center mb-3">
        <i class="bi bi-exclamation-triangle-fill me-2 fs-4"></i>
        <div>
            <strong><c:out value="${c360.openSuspenseCount}"/> open suspense case(s)</strong> totaling
            <strong><fmt:formatNumber value="${c360.openSuspenseAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></strong>.
            Immediate resolution required per RBI guidelines.
        </div>
    </div>
    </c:if>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-exclamation-triangle me-2"></i>Suspense Cases</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.suspenseCases}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>Case ID</th>
                                <th>Account</th>
                                <th>Reason</th>
                                <th class="text-end">Amount</th>
                                <th>Status</th>
                                <th>Created</th>
                                <th>Resolved</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="sc" items="${c360.suspenseCases}">
                            <tr>
                                <td><code><c:out value="${sc.caseId}"/></code></td>
                                <td><code><c:out value="${sc.accountNumber}"/></code> <small class="text-muted"><c:out value="${sc.accountName}"/></small></td>
                                <td><c:out value="${sc.reasonCode}"/> <c:if test="${not empty sc.reasonDetail}"><br/><small class="text-muted"><c:out value="${sc.reasonDetail}"/></small></c:if></td>
                                <td class="text-end fw-bold"><fmt:formatNumber value="${sc.amount}" type="number" minFractionDigits="2" maxFractionDigits="2"/> <small><c:out value="${sc.currency}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${sc.status == 'OPEN'}"><span class="badge bg-danger">OPEN</span></c:when>
                                        <c:when test="${sc.status == 'RESOLVED'}"><span class="badge bg-success">RESOLVED</span></c:when>
                                        <c:when test="${sc.status == 'REVERSED'}"><span class="badge bg-secondary">REVERSED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${sc.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${sc.createdAt}"/></td>
                                <td><c:out value="${sc.resolvedAt}" default="--"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No suspense cases found.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 6: RISK & GOVERNANCE                                          --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-risk" role="tabpanel">

    <%-- Frozen Accounts Alert --%>
    <c:if test="${not empty c360.riskSummary.frozenAccounts}">
    <div class="alert alert-danger d-flex align-items-center mb-3">
        <i class="bi bi-snow me-2 fs-4"></i>
        <strong><c:out value="${fn:length(c360.riskSummary.frozenAccounts)}"/> account(s) with active freeze.</strong>
    </div>
    </c:if>

    <div class="row">
        <%-- Hard Transaction Limits --%>
        <div class="col-md-6 mb-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-white"><h6 class="mb-0"><i class="bi bi-shield-fill-exclamation me-1"></i>Hard Transaction Limits</h6></div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty c360.riskSummary.hardLimits}">
                        <table class="table table-sm table-striped mb-0">
                            <thead><tr><th>Channel</th><th class="text-end">Max Amount</th><th>Active</th></tr></thead>
                            <tbody>
                                <c:forEach var="hl" items="${c360.riskSummary.hardLimits}">
                                <tr>
                                    <td><c:out value="${hl.channel}"/></td>
                                    <td class="text-end fw-bold"><fmt:formatNumber value="${hl.absoluteMaxAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                    <td><c:choose><c:when test="${hl.isActive}"><span class="badge bg-success">Yes</span></c:when><c:otherwise><span class="badge bg-secondary">No</span></c:otherwise></c:choose></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </c:when>
                        <c:otherwise><p class="text-muted text-center p-3">No hard limits configured.</p></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- Velocity Limits --%>
        <div class="col-md-6 mb-4">
            <div class="card shadow-sm h-100">
                <div class="card-header bg-white"><h6 class="mb-0"><i class="bi bi-speedometer me-1"></i>Velocity Limits</h6></div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty c360.riskSummary.velocityLimits}">
                        <table class="table table-sm table-striped mb-0">
                            <thead><tr><th>Account</th><th>Max Txn/hr</th><th class="text-end">Max Amt/hr</th><th>Active</th></tr></thead>
                            <tbody>
                                <c:forEach var="vl" items="${c360.riskSummary.velocityLimits}">
                                <tr>
                                    <td><c:choose><c:when test="${vl.accountNumber != null}"><code><c:out value="${vl.accountNumber}"/></code></c:when><c:otherwise><span class="text-muted">Tenant Default</span></c:otherwise></c:choose></td>
                                    <td><c:out value="${vl.maxTxnCountPerHour}"/></td>
                                    <td class="text-end"><fmt:formatNumber value="${vl.maxTotalAmountPerHour}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                    <td><c:choose><c:when test="${vl.isActive}"><span class="badge bg-success">Yes</span></c:when><c:otherwise><span class="badge bg-secondary">No</span></c:otherwise></c:choose></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </c:when>
                        <c:otherwise><p class="text-muted text-center p-3">No velocity limits configured.</p></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

    <%-- Fraud Alerts --%>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-exclamation-diamond me-2"></i>Fraud Alerts</h5></div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.riskSummary.fraudAlerts}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>Alert ID</th>
                                <th>Account</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Details</th>
                                <th>Observed</th>
                                <th>Created</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="fa" items="${c360.riskSummary.fraudAlerts}">
                            <tr>
                                <td><code><c:out value="${fa.id}"/></code></td>
                                <td><code><c:out value="${fa.accountNumber}"/></code></td>
                                <td><c:out value="${fa.alertType}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${fa.status == 'OPEN'}"><span class="badge bg-danger">OPEN</span></c:when>
                                        <c:when test="${fa.status == 'RESOLVED'}"><span class="badge bg-success">RESOLVED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${fa.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${fa.details}"/></td>
                                <td><c:out value="${fa.observedCount}"/> txns / <fmt:formatNumber value="${fa.observedAmount}" type="number" minFractionDigits="2" maxFractionDigits="2"/></td>
                                <td><c:out value="${fa.createdAt}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No fraud alerts found.</p></c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Frozen Accounts Detail --%>
    <c:if test="${not empty c360.riskSummary.frozenAccounts}">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-snow me-2"></i>Frozen Accounts</h5></div>
        <div class="card-body p-0">
            <table class="table table-striped mb-0">
                <thead class="table-dark"><tr><th>Account</th><th>Name</th><th>Freeze Level</th><th>Reason</th></tr></thead>
                <tbody>
                    <c:forEach var="fa" items="${c360.riskSummary.frozenAccounts}">
                    <tr>
                        <td><code><c:out value="${fa.accountNumber}"/></code></td>
                        <td><c:out value="${fa.accountName}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${fa.freezeLevel == 'FULL'}"><span class="badge bg-danger">FULL</span></c:when>
                                <c:otherwise><span class="badge bg-warning text-dark"><c:out value="${fa.freezeLevel}"/></span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${fa.freezeReason}" default="--"/></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    </c:if>

    <%-- Hard Ceiling Violations --%>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-exclamation-octagon me-2"></i>Hard Ceiling Violations</h5></div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty c360.riskSummary.hardCeilingViolations}">
                <table class="table table-striped mb-0">
                    <thead class="table-dark"><tr><th>ID</th><th>Action</th><th>Details</th><th>User</th><th>Timestamp</th></tr></thead>
                    <tbody>
                        <c:forEach var="v" items="${c360.riskSummary.hardCeilingViolations}">
                        <tr>
                            <td><code><c:out value="${v.id}"/></code></td>
                            <td><span class="badge bg-danger"><c:out value="${v.action}"/></span></td>
                            <td><c:out value="${v.details}"/></td>
                            <td><c:out value="${v.username}"/></td>
                            <td><c:out value="${v.timestamp}"/></td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No hard ceiling violations recorded.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 7: AUDIT TRAIL                                                 --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-audit" role="tabpanel">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-clock-history me-2"></i>Audit Trail (<c:out value="${fn:length(c360.auditTrail)}"/> entries)</h5>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty c360.auditTrail}">
                <div class="cbs-timeline">
                    <c:forEach var="al" items="${c360.auditTrail}">
                    <div class="cbs-timeline-item mb-3 p-3 border-start border-3 ${al.entity == 'CUSTOMER' ? 'border-primary' : al.entity == 'ACCOUNT' ? 'border-success' : 'border-info'} bg-light rounded">
                        <div class="d-flex justify-content-between">
                            <div>
                                <span class="badge ${al.entity == 'CUSTOMER' ? 'bg-primary' : al.entity == 'ACCOUNT' ? 'bg-success' : 'bg-info'}"><c:out value="${al.entity}"/></span>
                                <strong class="ms-2"><c:out value="${al.action}"/></strong>
                                <small class="text-muted ms-2">Entity ID: <c:out value="${al.entityId}"/></small>
                            </div>
                            <small class="text-muted"><c:out value="${al.timestamp}"/></small>
                        </div>
                        <div class="mt-1">
                            <c:if test="${not empty al.details}"><div class="small"><c:out value="${al.details}"/></div></c:if>
                            <c:if test="${not empty al.username}"><small class="text-muted"><i class="bi bi-person"></i> <c:out value="${al.username}"/></small></c:if>
                            <c:if test="${not empty al.ipAddress}"><small class="text-muted ms-2"><i class="bi bi-globe"></i> <c:out value="${al.ipAddress}"/></small></c:if>
                        </div>
                        <c:if test="${not empty al.oldValue || not empty al.newValue}">
                        <div class="mt-1 small">
                            <c:if test="${not empty al.oldValue}"><span class="text-danger">Old: <c:out value="${al.oldValue}"/></span></c:if>
                            <c:if test="${not empty al.oldValue && not empty al.newValue}"> &rarr; </c:if>
                            <c:if test="${not empty al.newValue}"><span class="text-success">New: <c:out value="${al.newValue}"/></span></c:if>
                        </div>
                        </c:if>
                    </div>
                    </c:forEach>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center">No audit entries found.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

</div><%-- /tab-content --%>

<%@ include file="../layout/footer.jsp" %>
