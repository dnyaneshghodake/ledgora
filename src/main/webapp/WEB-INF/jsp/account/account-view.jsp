<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-wallet2"></i> Account Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/accounts/${account.id}/edit" class="btn btn-outline-secondary">
            <i class="bi bi-pencil"></i> Edit
        </a>
        <a href="${pageContext.request.contextPath}/transactions/history/${account.accountNumber}" class="btn btn-outline-info">
            <i class="bi bi-clock-history"></i> Transaction History
        </a>
        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary">Back</a>
    </div>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Account Information</h5></div>
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">Account Number</td><td><code class="fs-5">${account.accountNumber}</code></td></tr>
                    <tr><td class="text-muted">Account Name</td><td>${account.accountName}</td></tr>
                    <tr><td class="text-muted">Type</td><td><span class="badge bg-info">${account.accountType}</span></td></tr>
                    <tr><td class="text-muted">Status</td><td>
                        <c:choose>
                            <c:when test="${account.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                            <c:when test="${account.status == 'SUSPENDED'}"><span class="badge bg-warning">SUSPENDED</span></c:when>
                            <c:otherwise><span class="badge bg-danger">${account.status}</span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Branch Code</td><td>${account.branchCode}</td></tr>
                    <tr><td class="text-muted">GL Account Code</td><td>${account.glAccountCode}</td></tr>
                    <tr><td class="text-muted">Created</td><td>${account.createdAt}</td></tr>
                </table>
            </div>
        </div>

        <%-- CBS Balance View - Grouped Cards --%>
        <div class="card shadow mt-3">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-graph-up"></i> CBS Balance View</h5></div>
            <div class="card-body">

                <%-- Ledger Section --%>
                <div class="cbs-balance-section">
                    <div class="cbs-balance-section-title">Ledger Section</div>
                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="cbs-balance-card cbs-balance-primary cbs-tooltip">
                                <div class="cbs-balance-label">Actual Total Balance</div>
                                <div class="cbs-balance-value">${account.balance} ${account.currency}</div>
                                <span class="cbs-tooltip-text">Total book balance including all posted entries</span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="cbs-balance-card cbs-tooltip">
                                <div class="cbs-balance-label">Actual Cleared Balance</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.clearedBalance}">${account.clearedBalance} ${account.currency}</c:when>
                                        <c:otherwise>${account.balance} ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Balance of cleared (settled) transactions only</span>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Operational Section --%>
                <div class="cbs-balance-section">
                    <div class="cbs-balance-section-title">Operational Section</div>
                    <div class="row g-3">
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-balance-warning cbs-tooltip">
                                <div class="cbs-balance-label">Shadow Total Balance</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.shadowBalance}">${account.shadowBalance} ${account.currency}</c:when>
                                        <c:otherwise>0.00 ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Projected balance including pending/authorized vouchers</span>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-tooltip">
                                <div class="cbs-balance-label">Inward Clearing Balance</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.inwardClearingBalance}">${account.inwardClearingBalance} ${account.currency}</c:when>
                                        <c:otherwise>0.00 ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Credits pending clearing (e.g. cheque deposits)</span>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-tooltip">
                                <div class="cbs-balance-label">Uncleared Effect</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.unclearedEffect}">${account.unclearedEffect} ${account.currency}</c:when>
                                        <c:otherwise>0.00 ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Difference between total and cleared balance</span>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Control Section --%>
                <div class="cbs-balance-section">
                    <div class="cbs-balance-section-title">Control Section</div>
                    <div class="row g-3">
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-balance-danger cbs-tooltip">
                                <div class="cbs-balance-label">Lien Balance</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.lienBalance}">${account.lienBalance} ${account.currency}</c:when>
                                        <c:otherwise>0.00 ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Amount marked under lien (blocked for specific purpose)</span>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-tooltip">
                                <div class="cbs-balance-label">Charge Hold</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.chargeHold}">${account.chargeHold} ${account.currency}</c:when>
                                        <c:otherwise>0.00 ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Amount reserved for pending charges/fees</span>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="cbs-balance-card cbs-balance-success cbs-tooltip">
                                <div class="cbs-balance-label">Available Balance</div>
                                <div class="cbs-balance-value">
                                    <c:choose>
                                        <c:when test="${not empty account.availableBalance}">${account.availableBalance} ${account.currency}</c:when>
                                        <c:otherwise>${account.balance} ${account.currency}</c:otherwise>
                                    </c:choose>
                                </div>
                                <span class="cbs-tooltip-text">Actual withdrawable amount after lien and holds</span>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Customer Info</h5></div>
            <div class="card-body">
                <p><strong>${account.customerName}</strong></p>
                <p><i class="bi bi-envelope"></i> ${account.customerEmail}</p>
                <p><i class="bi bi-telephone"></i> ${account.customerPhone}</p>
            </div>
        </div>
        <div class="card shadow mt-3">
            <div class="card-header bg-white"><h5 class="mb-0">Quick Actions</h5></div>
            <div class="card-body d-grid gap-2">
                <a href="${pageContext.request.contextPath}/transactions/deposit?account=${account.accountNumber}" class="btn btn-outline-success btn-sm cbs-lockable">Deposit</a>
                <a href="${pageContext.request.contextPath}/transactions/withdraw?account=${account.accountNumber}" class="btn btn-outline-danger btn-sm cbs-lockable">Withdraw</a>
                <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/status" class="d-inline">
                    <c:if test="${account.status == 'ACTIVE'}">
                        <input type="hidden" name="status" value="SUSPENDED"/>
                        <button type="submit" class="btn btn-outline-warning btn-sm w-100" onclick="return confirm('Suspend this account?')">Suspend</button>
                    </c:if>
                    <c:if test="${account.status == 'SUSPENDED'}">
                        <input type="hidden" name="status" value="ACTIVE"/>
                        <button type="submit" class="btn btn-outline-success btn-sm w-100">Reactivate</button>
                    </c:if>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
