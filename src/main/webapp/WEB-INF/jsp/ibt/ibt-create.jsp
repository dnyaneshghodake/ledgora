<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-building"></i> Inter-Branch Transfer</h3>
    <a href="${pageContext.request.contextPath}/ibt" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to IBT List</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${error}"/></div>
</c:if>

<%-- Info Banner --%>
<div class="alert alert-info border-0">
    <i class="bi bi-info-circle-fill me-2"></i>
    <strong>Inter-Branch Transfer</strong> &mdash;
    Use this form to transfer funds between accounts at <em>different</em> branches.
    The system will automatically create 4 vouchers (2 per branch) via the IBC clearing flow.
    For same-branch transfers, use the standard
    <a href="${pageContext.request.contextPath}/transactions/transfer">Transfer</a> screen.
</div>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/ibt/create" id="ibtForm"
              data-context-path="${pageContext.request.contextPath}">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="row g-3">

                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-up-circle text-danger"></i> Source Account (Debit)</strong></div>
                        <div class="card-body">
                            <label class="form-label cbs-field-required">Account Number</label>
                            <div class="input-group">
                                <input type="text" name="sourceAccountNumber" id="fromAccount" class="form-control" required readonly
                                       placeholder="Use lookup to select"/>
                                <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('fromAccount','fromAccountName')" title="Search Account">
                                    <i class="bi bi-search"></i>
                                </button>
                            </div>
                            <input type="hidden" id="fromAccountName"/>
                            <div class="mt-2" id="fromInfo"></div>
                            <div class="row g-2 mt-2" id="fromBalanceRow" style="display:none;">
                                <div class="col-6">
                                    <div class="card border-primary cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-primary">Available</small>
                                            <h6 id="fromAvailBalance" class="mb-0 text-primary">--</h6>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="card border-secondary cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-secondary">Branch</small>
                                            <h6 id="fromBranch" class="mb-0 text-secondary">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-down-circle text-success"></i> Destination Account (Credit)</strong></div>
                        <div class="card-body">
                            <label class="form-label cbs-field-required">Account Number</label>
                            <div class="input-group">
                                <input type="text" name="destinationAccountNumber" id="toAccount" class="form-control" required readonly
                                       placeholder="Use lookup to select"/>
                                <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('toAccount','toAccountName')" title="Search Account">
                                    <i class="bi bi-search"></i>
                                </button>
                            </div>
                            <input type="hidden" id="toAccountName"/>
                            <div class="mt-2" id="toInfo"></div>
                            <div class="row g-2 mt-2" id="toBalanceRow" style="display:none;">
                                <div class="col-6">
                                    <div class="card border-success cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-success">Available</small>
                                            <h6 id="toAvailBalance" class="mb-0 text-success">--</h6>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="card border-secondary cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-secondary">Branch</small>
                                            <h6 id="toBranch" class="mb-0 text-secondary">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Branch Mismatch Indicator --%>
                <div class="col-12" id="branchCheckRow" style="display:none;">
                    <div id="branchCheckOk" class="alert alert-success" style="display:none;">
                        <i class="bi bi-check-circle-fill"></i>
                        <strong>Cross-branch detected:</strong> <span id="branchCheckMsg"></span>
                        &mdash; 4-voucher IBC clearing flow will be used.
                    </div>
                    <div id="branchCheckFail" class="alert alert-warning" style="display:none;">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                        <strong>Same branch:</strong> Both accounts are at the same branch.
                        Please use the standard <a href="${pageContext.request.contextPath}/transactions/transfer">Transfer</a> screen.
                    </div>
                </div>

                <div class="col-md-5">
                    <label class="form-label cbs-field-required">Amount</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01" id="amountInput"
                           placeholder="Enter transfer amount"/>
                </div>
                <div class="col-md-7">
                    <label class="form-label">Narration</label>
                    <input type="text" name="narration" class="form-control" maxlength="500"
                           placeholder="e.g. Salary transfer from HQ to Downtown branch"/>
                </div>

                <div class="col-12"><hr>
                    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn">
                        <i class="bi bi-building"></i> Execute Inter-Branch Transfer
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>

<%-- AJAX account lookup callback to show branch info --%>
<script>
(function() {
    'use strict';
    var contextPath = '${pageContext.request.contextPath}';

    // Override the standard lookup callback to also show branch
    window.ibtAccountSelected = function(fieldId, accountNumber) {
        if (!accountNumber) return;
        fetch(contextPath + '/accounts/api/lookup?accountNumber=' + encodeURIComponent(accountNumber))
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var isFrom = (fieldId === 'fromAccount');
                var prefix = isFrom ? 'from' : 'to';
                var infoDiv = document.getElementById(prefix + 'Info');
                var balRow = document.getElementById(prefix + 'BalanceRow');
                var availEl = document.getElementById(prefix + 'AvailBalance');
                var branchEl = document.getElementById(prefix + 'Branch');

                if (data && data.accountNumber) {
                    if (infoDiv) infoDiv.innerHTML = '<small class="text-muted">' + (data.accountName || '') + ' &mdash; ' + (data.customerName || '') + '</small>';
                    if (availEl) availEl.textContent = data.availableBalance != null ? data.availableBalance : '--';
                    if (branchEl) branchEl.textContent = data.branchCode || '--';
                    if (balRow) balRow.style.display = '';
                }
                checkBranches();
            })
            .catch(function() {});
    };

    // After standard lookup sets the field, trigger our callback
    var origSetField = window.setAccountField;
    window.setAccountField = function(fieldId, nameFieldId, accountNumber) {
        if (origSetField) origSetField(fieldId, nameFieldId, accountNumber);
        // Trigger IBT branch check
        if (typeof window.ibtAccountSelected === 'function') {
            window.ibtAccountSelected(fieldId, accountNumber);
        }
    };

    function checkBranches() {
        var fromBranch = document.getElementById('fromBranch');
        var toBranch = document.getElementById('toBranch');
        var checkRow = document.getElementById('branchCheckRow');
        var okDiv = document.getElementById('branchCheckOk');
        var failDiv = document.getElementById('branchCheckFail');
        var msgSpan = document.getElementById('branchCheckMsg');

        if (!fromBranch || !toBranch) return;
        var fb = fromBranch.textContent.trim();
        var tb = toBranch.textContent.trim();
        if (fb === '--' || tb === '--') { if (checkRow) checkRow.style.display = 'none'; return; }

        if (checkRow) checkRow.style.display = '';
        if (fb !== tb) {
            if (okDiv) okDiv.style.display = '';
            if (failDiv) failDiv.style.display = 'none';
            if (msgSpan) msgSpan.textContent = fb + ' \u2192 ' + tb;
        } else {
            if (okDiv) okDiv.style.display = 'none';
            if (failDiv) failDiv.style.display = '';
        }
    }
})();
</script>

<script src="${pageContext.request.contextPath}/resources/js/transaction.js"></script>
<%@ include file="../layout/footer.jsp" %>
