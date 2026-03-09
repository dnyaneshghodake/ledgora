<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-wallet2"></i> Account Details</h3>
    <div>
        <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
        <a href="${pageContext.request.contextPath}/accounts/${account.id}/edit" class="btn btn-outline-secondary"><i class="bi bi-pencil"></i> Edit</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-primary"><i class="bi bi-arrow-left"></i> Back</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<c:if test="${account.freezeLevel != null && account.freezeLevel != 'NONE'}">
    <c:set var="freezeActive" value="${true}" scope="request"/>
    <c:set var="freezeLevel" value="${account.freezeLevel}" scope="request"/>
    <c:set var="freezeReason" value="${account.freezeReason}" scope="request"/>
</c:if>
<c:if test="${account.approvalStatus == 'PENDING'}">
    <c:set var="approvalPending" value="${true}" scope="request"/>
    <c:set var="approvalPendingMessage" value="This account is pending maker-checker approval." scope="request"/>
</c:if>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<ul class="nav nav-tabs mb-4" id="accountTabs">
    <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#tab-details">Details</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-balances">Balances</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-ownership">Ownership</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-liens">Liens</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-transactions">Transactions</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-freeze-history">Freeze History</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-lien-history">Lien History</a></li>
</ul>

<div class="tab-content">
<%-- Details Tab --%>
<div class="tab-pane fade show active" id="tab-details">
    <div class="card shadow">
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>Account Number:</strong> <code><c:out value="${account.accountNumber}"/></code></div>
                <div class="col-md-4"><strong>Account Name:</strong> <c:out value="${account.accountName}"/></div>
                <div class="col-md-4"><strong>Type:</strong> <span class="badge bg-info"><c:out value="${account.accountType}"/></span></div>
                <div class="col-md-4"><strong>Customer:</strong> <c:out value="${account.customerName}"/></div>
                <div class="col-md-4"><strong>Currency:</strong> <c:out value="${account.currency}"/></div>
                <div class="col-md-4">
                    <strong>Status:</strong>
                    <c:choose>
                        <c:when test="${account.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                        <c:when test="${account.status == 'FROZEN'}"><span class="badge bg-danger">FROZEN</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${account.status}"/></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>Freeze Level:</strong>
                    <c:choose>
                        <c:when test="${account.freezeLevel != null && account.freezeLevel != 'NONE'}"><span class="badge bg-danger"><c:out value="${account.freezeLevel}"/></span></c:when>
                        <c:otherwise><span class="badge bg-success">NONE</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>Interest Rate:</strong> <c:out value="${account.interestRate}"/>%</div>
                <div class="col-md-4"><strong>Overdraft Limit:</strong> <c:out value="${account.overdraftLimit}"/></div>
                <div class="col-md-4"><strong>GL Code:</strong> <c:out value="${account.glAccountCode}"/></div>
                <div class="col-md-4"><strong>Created:</strong> <c:out value="${account.createdAt}"/></div>
                <div class="col-md-4"><strong>Approval:</strong>
                    <c:choose>
                        <c:when test="${account.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                        <c:when test="${account.approvalStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${account.approvalStatus}"/></span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Balances Tab --%>
<div class="tab-pane fade" id="tab-balances">
    <div class="card shadow">
        <div class="card-body">
            <div class="row g-4">
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body text-center">
                            <h6 class="text-primary">Ledger Balance</h6>
                            <h3 class="fw-bold"><c:out value="${account.balance}"/> <small><c:out value="${account.currency}"/></small></h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body text-center">
                            <h6 class="text-success">Available Balance</h6>
                            <h3 class="fw-bold"><c:out value="${availableBalance != null ? availableBalance : account.balance}"/> <small><c:out value="${account.currency}"/></small></h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body text-center">
                            <h6 class="text-warning">Total Lien</h6>
                            <h3 class="fw-bold"><c:out value="${totalLien != null ? totalLien : '0.00'}"/> <small><c:out value="${account.currency}"/></small></h3>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body text-center">
                            <h6 class="text-info">Overdraft Limit</h6>
                            <h3 class="fw-bold"><c:out value="${account.overdraftLimit}"/> <small><c:out value="${account.currency}"/></small></h3>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Ownership Tab --%>
<div class="tab-pane fade" id="tab-ownership">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between">
            <h5 class="mb-0"><i class="bi bi-people"></i> Account Ownership</h5>
            <a href="${pageContext.request.contextPath}/ownerships/account/${account.id}" class="btn btn-sm btn-outline-primary">Manage Ownership</a>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty ownerships}">
                    <table class="table table-hover">
                        <thead><tr><th>Customer</th><th>Type</th><th>Percentage</th><th>Operational</th></tr></thead>
                        <tbody>
                            <c:forEach var="o" items="${ownerships}">
                            <tr>
                                <td><c:out value="${o.customerName}"/></td>
                                <td><span class="badge bg-info"><c:out value="${o.ownershipType}"/></span></td>
                                <td><c:out value="${o.ownershipPercentage}"/>%</td>
                                <td><c:choose><c:when test="${o.operational}"><span class="badge bg-success">Yes</span></c:when><c:otherwise><span class="badge bg-secondary">No</span></c:otherwise></c:choose></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise><p class="text-muted text-center">No ownership records.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Liens Tab --%>
<div class="tab-pane fade" id="tab-liens">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between">
            <h5 class="mb-0"><i class="bi bi-lock"></i> Account Liens</h5>
            <a href="${pageContext.request.contextPath}/liens/account/${account.id}" class="btn btn-sm btn-outline-primary">Manage Liens</a>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty liens}">
                    <table class="table table-hover">
                        <thead><tr><th>Amount</th><th>Type</th><th>Start</th><th>End</th><th>Status</th></tr></thead>
                        <tbody>
                            <c:forEach var="l" items="${liens}">
                            <tr>
                                <td class="fw-bold"><c:out value="${l.lienAmount}"/></td>
                                <td><span class="badge bg-info"><c:out value="${l.lienType}"/></span></td>
                                <td><c:out value="${l.startDate}"/></td>
                                <td><c:out value="${l.endDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${l.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary"><c:out value="${l.status}"/></span></c:otherwise>
                                    </c:choose>
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
</div>

<%-- Transactions Tab --%>
<div class="tab-pane fade" id="tab-transactions">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between">
            <h5 class="mb-0"><i class="bi bi-arrow-left-right"></i> Recent Transactions</h5>
            <a href="${pageContext.request.contextPath}/transactions?accountId=${account.id}" class="btn btn-sm btn-outline-primary">View All</a>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty recentTransactions}">
                    <table class="table table-hover table-sm">
                        <thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Balance After</th></tr></thead>
                        <tbody>
                            <c:forEach var="tx" items="${recentTransactions}">
                            <tr>
                                <td><small><c:out value="${tx.transactionDate}"/></small></td>
                                <td><c:out value="${tx.transactionType}"/></td>
                                <td class="fw-bold"><c:out value="${tx.amount}"/></td>
                                <td><c:out value="${tx.balanceAfter}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise><p class="text-muted text-center">No recent transactions.</p></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Freeze History Tab --%>
<div class="tab-pane fade" id="tab-freeze-history">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clock-history"></i> Freeze History</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty freezeHistory}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Action</th><th>Maker</th><th>Checker</th><th>Status</th><th>Details</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="fh" items="${freezeHistory}">
                                <tr>
                                    <td><small><c:out value="${fh.timestamp}"/></small></td>
                                    <td><span class="badge bg-info"><c:out value="${fh.action}"/></span></td>
                                    <td><c:out value="${fh.username}" default="System"/></td>
                                    <td><c:out value="${fh.checker}" default="--"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${fh.action == 'FREEZE_APPLY'}"><span class="badge bg-danger">Frozen</span></c:when>
                                            <c:when test="${fh.action == 'FREEZE_RELEASE'}"><span class="badge bg-success">Released</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary"><c:out value="${fh.action}"/></span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><small><c:out value="${fh.details}"/></small></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-clock-history" style="font-size: 2rem;"></i>
                        <p class="mt-2">No freeze history records found.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Lien History Tab --%>
<div class="tab-pane fade" id="tab-lien-history">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-lock-fill"></i> Lien History</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty lienHistory}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Action</th><th>Maker</th><th>Checker</th><th>Status</th><th>Amount</th><th>Details</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="lh" items="${lienHistory}">
                                <tr>
                                    <td><small><c:out value="${lh.timestamp}"/></small></td>
                                    <td><span class="badge bg-info"><c:out value="${lh.action}"/></span></td>
                                    <td><c:out value="${lh.username}" default="System"/></td>
                                    <td><c:out value="${lh.checker}" default="--"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${lh.action == 'LIEN_CREATE'}"><span class="badge bg-warning">Created</span></c:when>
                                            <c:when test="${lh.action == 'LIEN_APPROVE'}"><span class="badge bg-success">Approved</span></c:when>
                                            <c:when test="${lh.action == 'LIEN_RELEASE'}"><span class="badge bg-secondary">Released</span></c:when>
                                            <c:otherwise><span class="badge bg-info"><c:out value="${lh.action}"/></span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="fw-bold"><c:out value="${lh.amount}" default="--"/></td>
                                    <td><small><c:out value="${lh.details}"/></small></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-lock" style="font-size: 2rem;"></i>
                        <p class="mt-2">No lien history records found.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
</div>

<%-- Audit Info Section (uses eagerly-resolved model attributes to avoid LazyInitializationException) --%>
<c:set var="auditCreatedBy" value="${createdByUsername}" scope="request"/>
<c:set var="auditCreatedAt" value="${account.createdAt}" scope="request"/>
<c:set var="auditLastModifiedBy" value="" scope="request"/>
<c:set var="auditUpdatedAt" value="${account.updatedAt}" scope="request"/>
<c:set var="auditApprovedBy" value="${approvedByUsername}" scope="request"/>
<c:set var="auditApprovalStatus" value="${account.approvalStatus}" scope="request"/>
<c:set var="auditCurrentStatus" value="${account.status}" scope="request"/>
<c:set var="auditEntityType" value="Account" scope="request"/>
<c:set var="auditEntityId" value="${account.accountNumber}" scope="request"/>
<%@ include file="../layout/audit-info.jsp" %>

<%-- Activate tab from URL hash (e.g., #tab-balances) --%>
<script>
(function() {
    var hash = window.location.hash;
    if (hash) {
        var tabLink = document.querySelector('#accountTabs a[href="' + hash + '"]');
        if (tabLink) {
            var tab = new bootstrap.Tab(tabLink);
            tab.show();
        }
    }
    // Update URL hash when tabs are clicked
    var tabEls = document.querySelectorAll('#accountTabs a[data-bs-toggle="tab"]');
    tabEls.forEach(function(tabEl) {
        tabEl.addEventListener('shown.bs.tab', function(e) {
            history.replaceState(null, null, e.target.getAttribute('href'));
        });
    });
})();
</script>

<%@ include file="../layout/footer.jsp" %>
