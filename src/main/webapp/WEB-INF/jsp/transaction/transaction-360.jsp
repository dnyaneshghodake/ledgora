<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-eye-fill"></i> Transaction 360&deg; View</h3>
    <div>
        <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Back to List</a>
        <a href="${pageContext.request.contextPath}/transactions/${txn.transactionId}" class="btn btn-outline-primary btn-sm"><i class="bi bi-receipt"></i> Basic View</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- AUTHORIZATION PANEL (Checker View Only)                           --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<c:if test="${isAuthorizeView && txn.pendingApproval && !isAuditor}">
<div class="card border-warning mb-4 shadow cbs-auth-panel">
    <div class="card-header bg-warning text-dark d-flex align-items-center">
        <i class="bi bi-shield-check fs-4 me-2"></i>
        <strong class="fs-5">Authorization Required</strong>
        <span class="badge bg-dark ms-auto">PENDING APPROVAL</span>
    </div>
    <div class="card-body">
        <p class="mb-3 text-muted">
            Review all transaction details below before authorizing.
            CBS standard requires full transparency before approval.
            <strong>Maker &ne; Checker</strong> rule is enforced.
        </p>
        <div class="mb-3">
            <label for="checkerRemarks" class="form-label fw-bold">Checker Remarks</label>
            <textarea id="checkerRemarks" class="form-control" rows="3"
                      placeholder="Enter authorization remarks (required for reject, recommended for approve)..."
                      oninput="document.getElementById('approveRemarks').value=this.value; document.getElementById('rejectRemarks').value=this.value;"></textarea>
        </div>
        <div class="d-flex gap-2">
            <form method="post" action="${pageContext.request.contextPath}/transactions/${txn.transactionId}/approve" class="d-inline">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <input type="hidden" id="approveRemarks" name="remarks" value=""/>
                <button type="submit" class="btn btn-success btn-lg">
                    <i class="bi bi-check-circle-fill"></i> Approve
                </button>
            </form>
            <form method="post" action="${pageContext.request.contextPath}/transactions/${txn.transactionId}/reject" class="d-inline">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <input type="hidden" id="rejectRemarks" name="remarks" value=""/>
                <button type="submit" class="btn btn-danger btn-lg">
                    <i class="bi bi-x-circle-fill"></i> Reject
                </button>
            </form>
        </div>
    </div>
</div>
</c:if>

<c:if test="${isAuthorizeView && txn.pendingApproval && isAuditor}">
<div class="card border-info mb-4 shadow">
    <div class="card-header bg-info text-white d-flex align-items-center">
        <i class="bi bi-eye fs-4 me-2"></i>
        <strong class="fs-5">Audit Review (Read-Only)</strong>
    </div>
    <div class="card-body">
        <p class="mb-0 text-muted">You are viewing this transaction as an Auditor. Authorization actions are not available for audit-only roles.</p>
    </div>
</div>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- TAB NAVIGATION                                                    --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<ul class="nav nav-tabs cbs-360-tabs mb-0" id="txn360Tabs" role="tablist">
    <li class="nav-item" role="presentation">
        <button class="nav-link active" id="tab-overview" data-bs-toggle="tab" data-bs-target="#pane-overview" type="button" role="tab" aria-controls="pane-overview" aria-selected="true">
            <i class="bi bi-info-circle"></i> Overview
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-vouchers" data-bs-toggle="tab" data-bs-target="#pane-vouchers" type="button" role="tab" aria-controls="pane-vouchers" aria-selected="false">
            <i class="bi bi-journal-text"></i> Vouchers
            <span class="badge bg-secondary ms-1"><c:out value="${fn:length(txn.vouchers)}"/></span>
        </button>
    </li>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-ledger" data-bs-toggle="tab" data-bs-target="#pane-ledger" type="button" role="tab" aria-controls="pane-ledger" aria-selected="false">
            <i class="bi bi-journal-bookmark-fill"></i> Ledger
            <span class="badge bg-secondary ms-1"><c:out value="${fn:length(txn.ledgerEntries)}"/></span>
        </button>
    </li>
    <c:if test="${txn.ibtTransaction}">
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-ibt" data-bs-toggle="tab" data-bs-target="#pane-ibt" type="button" role="tab" aria-controls="pane-ibt" aria-selected="false">
            <i class="bi bi-arrow-left-right"></i> IBT
        </button>
    </li>
    </c:if>
    <c:if test="${txn.suspenseTransaction}">
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-suspense" data-bs-toggle="tab" data-bs-target="#pane-suspense" type="button" role="tab" aria-controls="pane-suspense" aria-selected="false">
            <i class="bi bi-exclamation-triangle"></i> Suspense
            <span class="badge bg-danger ms-1"><c:out value="${fn:length(txn.suspenseCases)}"/></span>
        </button>
    </li>
    </c:if>
    <li class="nav-item" role="presentation">
        <button class="nav-link" id="tab-audit" data-bs-toggle="tab" data-bs-target="#pane-audit" type="button" role="tab" aria-controls="pane-audit" aria-selected="false">
            <i class="bi bi-clock-history"></i> Audit
            <span class="badge bg-secondary ms-1"><c:out value="${fn:length(txn.auditTrail)}"/></span>
        </button>
    </li>
</ul>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- TAB CONTENT                                                       --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="tab-content cbs-360-tab-content" id="txn360TabContent">

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 1: OVERVIEW (Transaction Header + Account Impact)              --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade show active" id="pane-overview" role="tabpanel" aria-labelledby="tab-overview">

    <%-- Section 1: Transaction Header --%>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0"><i class="bi bi-receipt-cutoff me-2"></i>Transaction Header</h5>
            <div class="ms-auto">
                <c:choose>
                    <c:when test="${txn.status == 'COMPLETED'}"><span class="badge cbs-badge-completed fs-6">COMPLETED</span></c:when>
                    <c:when test="${txn.status == 'PENDING_APPROVAL'}"><span class="badge cbs-badge-pending fs-6">PENDING APPROVAL</span></c:when>
                    <c:when test="${txn.status == 'PARKED'}"><span class="badge cbs-badge-parked fs-6">PARKED</span></c:when>
                    <c:when test="${txn.status == 'REVERSED'}"><span class="badge cbs-badge-reversed fs-6">REVERSED</span></c:when>
                    <c:when test="${txn.status == 'APPROVED'}"><span class="badge cbs-badge-approved fs-6">APPROVED</span></c:when>
                    <c:when test="${txn.status == 'REJECTED'}"><span class="badge cbs-badge-rejected fs-6">REJECTED</span></c:when>
                    <c:when test="${txn.status == 'FAILED'}"><span class="badge cbs-badge-failed fs-6">FAILED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary fs-6"><c:out value="${txn.status}"/></span></c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">Transaction ID</td><td><code class="fs-5"><c:out value="${txn.transactionId}"/></code></td></tr>
                        <tr><td class="cbs-label">Reference</td><td><code><c:out value="${txn.transactionRef}"/></code></td></tr>
                        <tr><td class="cbs-label">Type</td><td>
                            <c:choose>
                                <c:when test="${txn.transactionType == 'DEPOSIT'}"><span class="badge bg-success">DEPOSIT</span></c:when>
                                <c:when test="${txn.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger">WITHDRAWAL</span></c:when>
                                <c:when test="${txn.transactionType == 'TRANSFER'}"><span class="badge bg-info">TRANSFER</span></c:when>
                                <c:when test="${txn.transactionType == 'REVERSAL'}"><span class="badge bg-secondary">REVERSAL</span></c:when>
                                <c:when test="${txn.transactionType == 'SETTLEMENT'}"><span class="badge bg-primary">SETTLEMENT</span></c:when>
                                <c:otherwise><span class="badge bg-dark"><c:out value="${txn.transactionType}"/></span></c:otherwise>
                            </c:choose>
                        </td></tr>
                        <tr><td class="cbs-label">Channel</td><td><c:choose><c:when test="${txn.channel != null}"><span class="badge bg-secondary"><c:out value="${txn.channel}"/></span></c:when><c:otherwise><span class="text-muted">N/A</span></c:otherwise></c:choose></td></tr>
                        <tr><td class="cbs-label">Amount</td><td><span class="fs-4 fw-bold text-primary"><c:out value="${txn.amount}"/> <c:out value="${txn.currency}"/></span></td></tr>
                        <tr><td class="cbs-label">Description</td><td><c:out value="${txn.description}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Narration</td><td><c:out value="${txn.narration}" default="--"/></td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">Business Date</td><td><c:out value="${txn.businessDate}"/></td></tr>
                        <tr><td class="cbs-label">Value Date</td><td><c:out value="${txn.valueDate}"/></td></tr>
                        <tr><td class="cbs-label">Branch Code</td><td><code><c:out value="${txn.branchCode}" default="--"/></code></td></tr>
                        <tr><td class="cbs-label">Tenant Code</td><td><code><c:out value="${txn.tenantCode}" default="--"/></code></td></tr>
                        <tr><td class="cbs-label">Initiated By (Maker)</td><td>
                            <c:choose>
                                <c:when test="${txn.makerUsername != null}"><i class="bi bi-person-fill text-primary"></i> <c:out value="${txn.makerUsername}"/>
                                    <c:if test="${txn.makerTimestamp != null}"><br/><small class="text-muted"><c:out value="${txn.makerTimestamp}"/></small></c:if>
                                </c:when>
                                <c:otherwise><span class="text-muted">System</span></c:otherwise>
                            </c:choose>
                        </td></tr>
                        <tr><td class="cbs-label">Authorized By (Checker)</td><td>
                            <c:choose>
                                <c:when test="${txn.checkerUsername != null}"><i class="bi bi-person-check-fill text-success"></i> <c:out value="${txn.checkerUsername}"/>
                                    <c:if test="${txn.checkerTimestamp != null}"><br/><small class="text-muted"><c:out value="${txn.checkerTimestamp}"/></small></c:if>
                                    <c:if test="${txn.checkerRemarks != null}"><br/><small class="text-muted">Remarks: <c:out value="${txn.checkerRemarks}"/></small></c:if>
                                </c:when>
                                <c:when test="${txn.pendingApproval}"><span class="badge bg-warning text-dark"><i class="bi bi-hourglass-split"></i> Awaiting</span></c:when>
                                <c:otherwise><span class="text-muted">Auto-authorized</span></c:otherwise>
                            </c:choose>
                        </td></tr>
                        <tr><td class="cbs-label">Created At</td><td><c:out value="${txn.createdAt}"/></td></tr>
                    </table>
                </div>
            </div>

            <%-- Reversal Linkage --%>
            <c:if test="${txn.reversalOfTransactionId != null}">
            <div class="alert alert-secondary mt-3">
                <i class="bi bi-arrow-return-left"></i>
                <strong>Reversal of Transaction:</strong>
                <a href="${pageContext.request.contextPath}/transactions/${txn.reversalOfTransactionId}/view">
                    <c:out value="${txn.reversalOfTransactionRef}"/> (ID: <c:out value="${txn.reversalOfTransactionId}"/>)
                </a>
            </div>
            </c:if>
        </div>
    </div>

    <%-- Section 2: Account Impact Summary --%>
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-bank me-2"></i>Account Impact Summary</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty txn.accountImpacts}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>Account Number</th>
                                <th>Account Name</th>
                                <th>Branch</th>
                                <th>DR/CR</th>
                                <th class="text-end">Amount</th>
                                <th class="text-end">Pre-Balance</th>
                                <th class="text-end">Post-Balance</th>
                                <th>Freeze</th>
                                <th class="text-end">Lien</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="impact" items="${txn.accountImpacts}">
                            <tr>
                                <td><code><c:out value="${impact.accountNumber}"/></code></td>
                                <td><c:out value="${impact.accountName}"/></td>
                                <td><c:out value="${impact.branchCode}" default="--"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${impact.drCr == 'DR'}"><span class="badge cbs-dr">DR</span></c:when>
                                        <c:when test="${impact.drCr == 'CR'}"><span class="badge cbs-cr">CR</span></c:when>
                                        <c:otherwise><c:out value="${impact.drCr}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end fw-bold"><c:out value="${impact.amount}"/></td>
                                <td class="text-end"><c:out value="${impact.preBalance}" default="--"/></td>
                                <td class="text-end"><c:out value="${impact.postBalance}" default="--"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${impact.freezeLevel == 'NONE'}"><span class="text-muted">NONE</span></c:when>
                                        <c:when test="${impact.freezeLevel == 'FULL'}"><span class="badge bg-danger">FULL</span></c:when>
                                        <c:otherwise><span class="badge bg-warning text-dark"><c:out value="${impact.freezeLevel}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end"><c:out value="${impact.lienAmount}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No account impacts recorded.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 2: VOUCHERS (Expandable Grid)                                  --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-vouchers" role="tabpanel" aria-labelledby="tab-vouchers">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-journal-text me-2"></i>Voucher Details</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty txn.vouchers}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th></th>
                                <th>Voucher Number</th>
                                <th>Branch</th>
                                <th>DR/CR</th>
                                <th>Account</th>
                                <th>GL Code</th>
                                <th class="text-end">Amount</th>
                                <th>Auth</th>
                                <th>Post</th>
                                <th>Cancel</th>
                                <th>Batch ID</th>
                                <th>Posting Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="v" items="${txn.vouchers}" varStatus="vs">
                            <tr class="cbs-voucher-row" data-bs-toggle="collapse" data-bs-target="#voucherDetail${vs.index}" aria-expanded="false" style="cursor:pointer;">
                                <td><i class="bi bi-chevron-right cbs-expand-icon"></i></td>
                                <td><code><c:out value="${v.voucherNumber}"/></code></td>
                                <td><c:out value="${v.branchCode}" default="--"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${v.drCr == 'DR'}"><span class="badge cbs-dr">DR</span></c:when>
                                        <c:when test="${v.drCr == 'CR'}"><span class="badge cbs-cr">CR</span></c:when>
                                        <c:otherwise><c:out value="${v.drCr}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><code><c:out value="${v.accountNumber}"/></code></td>
                                <td><c:out value="${v.glCode}" default="--"/></td>
                                <td class="text-end fw-bold"><c:out value="${v.amount}"/></td>
                                <td><c:choose><c:when test="${v.authFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                                <td><c:choose><c:when test="${v.postFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                                <td><c:choose><c:when test="${v.cancelFlag == 'Y'}"><span class="badge bg-danger">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                                <td><c:out value="${v.batchId}" default="--"/></td>
                                <td><c:out value="${v.postingDate}"/></td>
                            </tr>
                            <tr class="collapse" id="voucherDetail${vs.index}">
                                <td colspan="12" class="bg-light p-3">
                                    <div class="row">
                                        <div class="col-md-4">
                                            <h6 class="text-muted mb-2"><i class="bi bi-link-45deg"></i> Linked Ledger Entry</h6>
                                            <c:choose>
                                                <c:when test="${v.ledgerEntryId != null}">
                                                    <table class="table table-sm table-borderless mb-0">
                                                        <tr><td class="text-muted">Entry ID</td><td><code><c:out value="${v.ledgerEntryId}"/></code></td></tr>
                                                        <tr><td class="text-muted">Type</td><td>
                                                            <c:choose>
                                                                <c:when test="${v.ledgerEntryType == 'DEBIT'}"><span class="badge cbs-dr">DEBIT</span></c:when>
                                                                <c:when test="${v.ledgerEntryType == 'CREDIT'}"><span class="badge cbs-cr">CREDIT</span></c:when>
                                                            </c:choose>
                                                        </td></tr>
                                                        <tr><td class="text-muted">Amount</td><td><c:out value="${v.ledgerAmount}"/></td></tr>
                                                        <tr><td class="text-muted">Balance After</td><td><c:out value="${v.balanceAfter}"/></td></tr>
                                                    </table>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">No ledger entry linked (not yet posted)</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="col-md-4">
                                            <h6 class="text-muted mb-2"><i class="bi bi-arrow-return-left"></i> Reversal / Scroll</h6>
                                            <table class="table table-sm table-borderless mb-0">
                                                <tr><td class="text-muted">Reversal Of</td><td>
                                                    <c:choose>
                                                        <c:when test="${v.reversalOfVoucherId != null}">
                                                            <code><c:out value="${v.reversalOfVoucherNumber}"/></code>
                                                        </c:when>
                                                        <c:otherwise><span class="text-muted">N/A</span></c:otherwise>
                                                    </c:choose>
                                                </td></tr>
                                                <tr><td class="text-muted">Scroll No</td><td><c:out value="${v.scrollNo}" default="--"/></td></tr>
                                                <tr><td class="text-muted">Status</td><td><c:out value="${v.status}" default="--"/></td></tr>
                                            </table>
                                        </div>
                                        <div class="col-md-4">
                                            <h6 class="text-muted mb-2"><i class="bi bi-people"></i> Maker / Checker</h6>
                                            <table class="table table-sm table-borderless mb-0">
                                                <tr><td class="text-muted">Maker</td><td><i class="bi bi-person-fill text-primary"></i> <c:out value="${v.makerUsername}" default="--"/></td></tr>
                                                <tr><td class="text-muted">Checker</td><td>
                                                    <c:choose>
                                                        <c:when test="${v.checkerUsername != null}"><i class="bi bi-person-check-fill text-success"></i> <c:out value="${v.checkerUsername}"/></c:when>
                                                        <c:otherwise><span class="text-muted">Pending</span></c:otherwise>
                                                    </c:choose>
                                                </td></tr>
                                            </table>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No vouchers found for this transaction.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 3: LEDGER (Immutable View)                                     --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-ledger" role="tabpanel" aria-labelledby="tab-ledger">

    <%-- Immutable Ledger Warning Banner --%>
    <div class="alert cbs-immutable-banner mb-3" role="alert">
        <i class="bi bi-shield-lock-fill me-2"></i>
        <strong>Ledger entries are immutable.</strong> Corrections via reversal only. No ledger entry may be modified or deleted after creation.
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-journal-bookmark-fill me-2"></i>Ledger Entries</h5>
        </div>
        <div class="card-body p-0">
            <c:choose>
                <c:when test="${not empty txn.ledgerEntries}">
                <div class="table-responsive">
                    <table class="table table-striped table-hover mb-0 cbs-grid">
                        <thead class="table-dark">
                            <tr>
                                <th>Journal ID</th>
                                <th>Entry ID</th>
                                <th>Entry Type</th>
                                <th>GL Code</th>
                                <th class="text-end">Amount</th>
                                <th class="text-end">Balance After</th>
                                <th>Business Date</th>
                                <th>Voucher ID</th>
                                <th>Reversal Of</th>
                                <th>Posting Time</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="le" items="${txn.ledgerEntries}">
                            <tr>
                                <td><code><c:out value="${le.journalId}" default="--"/></code></td>
                                <td><code><c:out value="${le.id}"/></code></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${le.entryType == 'DEBIT'}"><span class="badge cbs-dr">DEBIT</span></c:when>
                                        <c:when test="${le.entryType == 'CREDIT'}"><span class="badge cbs-cr">CREDIT</span></c:when>
                                        <c:otherwise><c:out value="${le.entryType}"/></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${le.glCode}" default="--"/></td>
                                <td class="text-end fw-bold"><c:out value="${le.amount}"/></td>
                                <td class="text-end"><c:out value="${le.balanceAfter}"/></td>
                                <td><c:out value="${le.businessDate}"/></td>
                                <td><c:out value="${le.voucherId}" default="--"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${le.reversalOfEntryId != null}"><code><c:out value="${le.reversalOfEntryId}"/></code></c:when>
                                        <c:otherwise><span class="text-muted">--</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${le.postingTime}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center p-3">No ledger entries for this transaction.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 4: IBT PANEL (If applicable)                                   --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<c:if test="${txn.ibtTransaction}">
<div class="tab-pane fade" id="pane-ibt" role="tabpanel" aria-labelledby="tab-ibt">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0"><i class="bi bi-arrow-left-right me-2"></i>Inter-Branch Transfer</h5>
            <c:if test="${txn.ibtDetail != null}">
            <div class="ms-auto">
                <c:choose>
                    <c:when test="${txn.ibtDetail.status == 'INITIATED'}"><span class="badge bg-secondary fs-6">INITIATED</span></c:when>
                    <c:when test="${txn.ibtDetail.status == 'SENT'}"><span class="badge bg-info fs-6">SENT</span></c:when>
                    <c:when test="${txn.ibtDetail.status == 'RECEIVED'}"><span class="badge bg-primary fs-6">RECEIVED</span></c:when>
                    <c:when test="${txn.ibtDetail.status == 'SETTLED'}"><span class="badge bg-success fs-6">SETTLED</span></c:when>
                    <c:when test="${txn.ibtDetail.status == 'FAILED'}"><span class="badge bg-danger fs-6">FAILED</span></c:when>
                </c:choose>
            </div>
            </c:if>
        </div>
        <div class="card-body">
            <c:if test="${txn.ibtDetail != null}">

            <%-- IBT Status Timeline --%>
            <div class="cbs-ibt-timeline mb-4">
                <div class="d-flex justify-content-between align-items-center">
                    <div class="cbs-ibt-step ${txn.ibtDetail.status == 'INITIATED' || txn.ibtDetail.status == 'SENT' || txn.ibtDetail.status == 'RECEIVED' || txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-step-active' : ''}">
                        <div class="cbs-ibt-step-circle"><i class="bi bi-plus-circle"></i></div>
                        <small>INITIATED</small>
                    </div>
                    <div class="cbs-ibt-step-line ${txn.ibtDetail.status == 'SENT' || txn.ibtDetail.status == 'RECEIVED' || txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-line-active' : ''}"></div>
                    <div class="cbs-ibt-step ${txn.ibtDetail.status == 'SENT' || txn.ibtDetail.status == 'RECEIVED' || txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-step-active' : ''}">
                        <div class="cbs-ibt-step-circle"><i class="bi bi-send"></i></div>
                        <small>SENT</small>
                    </div>
                    <div class="cbs-ibt-step-line ${txn.ibtDetail.status == 'RECEIVED' || txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-line-active' : ''}"></div>
                    <div class="cbs-ibt-step ${txn.ibtDetail.status == 'RECEIVED' || txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-step-active' : ''}">
                        <div class="cbs-ibt-step-circle"><i class="bi bi-inbox"></i></div>
                        <small>RECEIVED</small>
                    </div>
                    <div class="cbs-ibt-step-line ${txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-line-active' : ''}"></div>
                    <div class="cbs-ibt-step ${txn.ibtDetail.status == 'SETTLED' ? 'cbs-ibt-step-active' : ''}">
                        <div class="cbs-ibt-step-circle"><i class="bi bi-check-circle"></i></div>
                        <small>SETTLED</small>
                    </div>
                </div>
            </div>

            <%-- IBT Header Details --%>
            <div class="row mb-4">
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">IBT ID</td><td><code><c:out value="${txn.ibtDetail.id}"/></code></td></tr>
                        <tr><td class="cbs-label">From Branch</td><td><strong><c:out value="${txn.ibtDetail.fromBranchCode}"/></strong> - <c:out value="${txn.ibtDetail.fromBranchName}"/></td></tr>
                        <tr><td class="cbs-label">To Branch</td><td><strong><c:out value="${txn.ibtDetail.toBranchCode}"/></strong> - <c:out value="${txn.ibtDetail.toBranchName}"/></td></tr>
                        <tr><td class="cbs-label">Amount</td><td class="fw-bold"><c:out value="${txn.ibtDetail.amount}"/> <c:out value="${txn.ibtDetail.currency}"/></td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <table class="table table-sm table-borderless cbs-detail-table">
                        <tr><td class="cbs-label">Business Date</td><td><c:out value="${txn.ibtDetail.businessDate}"/></td></tr>
                        <tr><td class="cbs-label">Settlement Date</td><td><c:out value="${txn.ibtDetail.settlementDate}" default="Pending"/></td></tr>
                        <tr><td class="cbs-label">Created By</td><td><c:out value="${txn.ibtDetail.createdByUsername}" default="--"/></td></tr>
                        <tr><td class="cbs-label">Approved By</td><td><c:out value="${txn.ibtDetail.approvedByUsername}" default="Pending"/></td></tr>
                    </table>
                </div>
            </div>

            <c:if test="${txn.ibtDetail.failureReason != null}">
            <div class="alert alert-danger"><i class="bi bi-exclamation-triangle-fill"></i> <strong>Failure:</strong> <c:out value="${txn.ibtDetail.failureReason}"/></div>
            </c:if>

            <%-- Branch A Vouchers --%>
            <h6 class="mt-3"><i class="bi bi-building"></i> Branch A Vouchers (<c:out value="${txn.ibtDetail.fromBranchCode}"/>)</h6>
            <c:choose>
                <c:when test="${not empty txn.ibtDetail.branchAVouchers}">
                <div class="table-responsive">
                    <table class="table table-sm table-striped mb-3 cbs-grid">
                        <thead class="table-secondary">
                            <tr><th>Voucher</th><th>DR/CR</th><th>Account</th><th>GL Code</th><th class="text-end">Amount</th><th>Auth</th><th>Post</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="bv" items="${txn.ibtDetail.branchAVouchers}">
                            <tr>
                                <td><code><c:out value="${bv.voucherNumber}"/></code></td>
                                <td><c:choose><c:when test="${bv.drCr == 'DR'}"><span class="badge cbs-dr">DR</span></c:when><c:otherwise><span class="badge cbs-cr">CR</span></c:otherwise></c:choose></td>
                                <td><code><c:out value="${bv.accountNumber}"/></code></td>
                                <td><c:out value="${bv.glCode}" default="--"/></td>
                                <td class="text-end fw-bold"><c:out value="${bv.amount}"/></td>
                                <td><c:choose><c:when test="${bv.authFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                                <td><c:choose><c:when test="${bv.postFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted">No vouchers for Branch A.</p></c:otherwise>
            </c:choose>

            <%-- Branch B Vouchers --%>
            <h6 class="mt-3"><i class="bi bi-building"></i> Branch B Vouchers (<c:out value="${txn.ibtDetail.toBranchCode}"/>)</h6>
            <c:choose>
                <c:when test="${not empty txn.ibtDetail.branchBVouchers}">
                <div class="table-responsive">
                    <table class="table table-sm table-striped mb-3 cbs-grid">
                        <thead class="table-secondary">
                            <tr><th>Voucher</th><th>DR/CR</th><th>Account</th><th>GL Code</th><th class="text-end">Amount</th><th>Auth</th><th>Post</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="bv" items="${txn.ibtDetail.branchBVouchers}">
                            <tr>
                                <td><code><c:out value="${bv.voucherNumber}"/></code></td>
                                <td><c:choose><c:when test="${bv.drCr == 'DR'}"><span class="badge cbs-dr">DR</span></c:when><c:otherwise><span class="badge cbs-cr">CR</span></c:otherwise></c:choose></td>
                                <td><code><c:out value="${bv.accountNumber}"/></code></td>
                                <td><c:out value="${bv.glCode}" default="--"/></td>
                                <td class="text-end fw-bold"><c:out value="${bv.amount}"/></td>
                                <td><c:choose><c:when test="${bv.authFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                                <td><c:choose><c:when test="${bv.postFlag == 'Y'}"><span class="badge bg-success">Y</span></c:when><c:otherwise><span class="badge bg-secondary">N</span></c:otherwise></c:choose></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted">No vouchers for Branch B.</p></c:otherwise>
            </c:choose>

            </c:if>
        </div>
    </div>
</div>
</c:if>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 5: SUSPENSE PANEL (If applicable)                              --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<c:if test="${txn.suspenseTransaction}">
<div class="tab-pane fade" id="pane-suspense" role="tabpanel" aria-labelledby="tab-suspense">
    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-exclamation-triangle me-2"></i>Suspense Cases</h5>
        </div>
        <div class="card-body">
            <c:forEach var="sc" items="${txn.suspenseCases}" varStatus="scs">
            <div class="card border-warning mb-3">
                <div class="card-header bg-warning bg-opacity-10 d-flex align-items-center">
                    <strong>Case #<c:out value="${sc.id}"/></strong>
                    <div class="ms-auto">
                        <c:choose>
                            <c:when test="${sc.status == 'OPEN'}"><span class="badge bg-warning text-dark">OPEN</span></c:when>
                            <c:when test="${sc.status == 'RESOLVED'}"><span class="badge bg-success">RESOLVED</span></c:when>
                            <c:when test="${sc.status == 'REVERSED'}"><span class="badge bg-secondary">REVERSED</span></c:when>
                        </c:choose>
                    </div>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6">
                            <table class="table table-sm table-borderless cbs-detail-table">
                                <tr><td class="cbs-label">Reason Code</td><td><code><c:out value="${sc.reasonCode}"/></code></td></tr>
                                <tr><td class="cbs-label">Detail</td><td><c:out value="${sc.reasonDetail}" default="--"/></td></tr>
                                <tr><td class="cbs-label">Suspense Account</td><td><code><c:out value="${sc.suspenseAccountNumber}"/></code> - <c:out value="${sc.suspenseAccountName}"/></td></tr>
                                <tr><td class="cbs-label">Intended Account</td><td><code><c:out value="${sc.intendedAccountNumber}"/></code> - <c:out value="${sc.intendedAccountName}"/></td></tr>
                                <tr><td class="cbs-label">Amount Parked</td><td class="fw-bold"><c:out value="${sc.amount}"/> <c:out value="${sc.currency}"/></td></tr>
                            </table>
                        </div>
                        <div class="col-md-6">
                            <table class="table table-sm table-borderless cbs-detail-table">
                                <tr><td class="cbs-label">Business Date</td><td><c:out value="${sc.businessDate}"/></td></tr>
                                <tr><td class="cbs-label">Created At</td><td><c:out value="${sc.createdAt}"/></td></tr>
                                <tr><td class="cbs-label">Posted Voucher</td><td><code><c:out value="${sc.postedVoucherNumber}" default="--"/></code></td></tr>
                                <tr><td class="cbs-label">Suspense Voucher</td><td><code><c:out value="${sc.suspenseVoucherNumber}" default="--"/></code></td></tr>
                                <c:if test="${sc.status != 'OPEN'}">
                                <tr><td class="cbs-label">Resolution Voucher</td><td><code><c:out value="${sc.resolutionVoucherNumber}" default="--"/></code></td></tr>
                                <tr><td class="cbs-label">Resolved By</td><td><c:out value="${sc.resolvedByUsername}" default="--"/></td></tr>
                                <tr><td class="cbs-label">Resolution Checker</td><td><c:out value="${sc.resolutionCheckerUsername}" default="--"/></td></tr>
                                <tr><td class="cbs-label">Resolved At</td><td><c:out value="${sc.resolvedAt}" default="--"/></td></tr>
                                <tr><td class="cbs-label">Remarks</td><td><c:out value="${sc.resolutionRemarks}" default="--"/></td></tr>
                                </c:if>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            </c:forEach>
        </div>
    </div>
</div>
</c:if>

<%-- ─────────────────────────────────────────────────────────────────── --%>
<%-- TAB 6: AUDIT TRAIL (Timeline Format)                               --%>
<%-- ─────────────────────────────────────────────────────────────────── --%>
<div class="tab-pane fade" id="pane-audit" role="tabpanel" aria-labelledby="tab-audit">

    <div class="alert alert-light border mb-3">
        <i class="bi bi-info-circle"></i> Audit trail is <strong>read-only</strong>. All entries are immutable and hash-chain verified.
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-clock-history me-2"></i>Audit Timeline</h5>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty txn.auditTrail}">
                <div class="cbs-audit-timeline">
                    <c:forEach var="audit" items="${txn.auditTrail}">
                    <div class="cbs-audit-entry">
                        <div class="cbs-audit-dot"></div>
                        <div class="cbs-audit-content">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <span class="badge bg-dark me-1"><c:out value="${audit.action}"/></span>
                                    <c:if test="${audit.username != null}">
                                        <span class="text-muted">by <strong><c:out value="${audit.username}"/></strong></span>
                                    </c:if>
                                </div>
                                <small class="text-muted"><c:out value="${audit.timestamp}"/></small>
                            </div>
                            <c:if test="${audit.details != null}">
                                <p class="mb-1 mt-1"><c:out value="${audit.details}"/></p>
                            </c:if>
                            <c:if test="${audit.ipAddress != null}">
                                <small class="text-muted">IP: <c:out value="${audit.ipAddress}"/></small>
                            </c:if>
                            <c:if test="${audit.oldValue != null || audit.newValue != null}">
                            <div class="mt-1">
                                <c:if test="${audit.oldValue != null}">
                                    <small class="d-block"><span class="text-danger">Old:</span> <code><c:out value="${audit.oldValue}"/></code></small>
                                </c:if>
                                <c:if test="${audit.newValue != null}">
                                    <small class="d-block"><span class="text-success">New:</span> <code><c:out value="${audit.newValue}"/></code></small>
                                </c:if>
                            </div>
                            </c:if>
                        </div>
                    </div>
                    </c:forEach>
                </div>
                </c:when>
                <c:otherwise><p class="text-muted text-center">No audit trail entries for this transaction.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

</div><%-- end tab-content --%>

<%-- Audit Footer Disclaimer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All ledger entries are immutable. System of Record: LedgerEntries. Corrections via reversal only.
</div>

<%-- Voucher expand/collapse toggle script --%>
<script>
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.cbs-voucher-row').forEach(function(row) {
        row.addEventListener('click', function() {
            var icon = this.querySelector('.cbs-expand-icon');
            if (icon) {
                icon.classList.toggle('bi-chevron-right');
                icon.classList.toggle('bi-chevron-down');
            }
        });
    });
});
</script>

<%@ include file="../layout/footer.jsp" %>
