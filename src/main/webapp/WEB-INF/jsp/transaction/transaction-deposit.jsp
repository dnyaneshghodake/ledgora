<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-stack"></i> Deposit</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/transactions/deposit" id="depositForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

            <div class="row g-3">
                                <%-- Account Lookup --%>
                                <div class="col-md-6">
                                    <label class="form-label cbs-field-required">Account Number</label>
                                    <div class="input-group">
                                        <input type="text" name="accountNumber" id="accountNumber" class="form-control" required readonly
                                               value="<c:out value="${param.accountNumber}"/>" placeholder="Use lookup to select account"/>
                                        <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('accountNumber','accountNameDisplay')" title="Search Account">
                                            <i class="bi bi-search"></i>
                                        </button>
                                    </div>
                                    <div id="acctInlineError" class="cbs-inline-error">Please select an account using the lookup.</div>
                                </div>

                                <%-- Account Info Display --%>
                                <div class="col-md-6">
                                    <label class="form-label">Account Name</label>
                                    <input type="text" class="form-control" id="accountNameDisplay" disabled placeholder="Auto-filled on selection"/>
                                </div>

                                <%-- Holiday Warning --%>
                                <c:if test="${isHoliday}">
                                <div class="col-12">
                                    <div class="cbs-txn-holiday-warning">
                                        <i class="bi bi-calendar-x"></i>
                                        <span>Today is a bank holiday. Transactions are restricted.</span>
                                    </div>
                                </div>
                                </c:if>

                                <%-- Balance Display --%>
                                <div class="col-12 cbs-txn-balance-row">
                                    <div class="row g-2">
                                        <div class="col-md-3">
                                            <div class="card border-primary cbs-txn-balance-card">
                                                <div class="card-body text-center p-2">
                                                    <small class="text-primary">Ledger Balance</small>
                                                    <h5 id="ledgerBalance" class="mb-0 text-primary">--</h5>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <div class="card border-success cbs-txn-balance-card">
                                                <div class="card-body text-center p-2">
                                                    <small class="text-success">Available Balance</small>
                                                    <h5 id="availableBalance" class="mb-0 text-success">--</h5>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <div class="card border-warning cbs-txn-balance-card">
                                                <div class="card-body text-center p-2">
                                                    <small class="text-warning">Lien Amount</small>
                                                    <h5 id="lienAmount" class="mb-0 text-warning">--</h5>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <div class="card border-info cbs-txn-balance-card">
                                                <div class="card-body text-center p-2">
                                                    <small class="text-info">Business Date</small>
                                                    <h5 id="businessDate" class="mb-0 text-info"><c:out value="${businessDate}" default="--"/></h5>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <%-- Freeze Warning --%>
                                <div class="col-12" id="freezeWarning" style="display:none;">
                                    <div class="cbs-txn-freeze-warning">
                                        <i class="bi bi-slash-circle"></i>
                                        <strong>FREEZE ACTIVE</strong> &mdash; <span id="freezeMsg"></span>
                                    </div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label cbs-field-required">Amount</label>
                                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01"
                                           placeholder="Enter amount (must be > 0)" id="amountInput"/>
                                    <div id="amtInlineError" class="cbs-inline-error">Amount must be greater than zero.</div>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">Description</label>
                                    <input type="text" name="description" class="form-control" maxlength="255" placeholder="Transaction description"/>
                                </div>

                                <div class="col-12">
                                    <hr>
                                    <button type="submit" class="btn btn-success btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}>
                                        <i class="bi bi-cash-stack"></i> Submit Deposit
                                    </button>
                                    <c:if test="${isHoliday}">
                                        <small class="text-danger ms-3"><i class="bi bi-info-circle"></i> Submissions disabled on holidays.</small>
                                    </c:if>
                                </div>
            </div>
        </form>
    </div>
</div>

<script>
// Auto-refresh balance when account is selected via lookup modal
document.getElementById('accountNumber').addEventListener('change', function() {
    refreshAccountBalance();
});

function refreshAccountBalance() {
    var num = document.getElementById('accountNumber').value;
    if (!num) return;

    // Reset previous state
    document.getElementById('freezeWarning').style.display = 'none';
    document.getElementById('submitBtn').disabled = false;

    fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data) {
                document.getElementById('accountNameDisplay').value = data.accountName || '';
                document.getElementById('ledgerBalance').textContent = formatCurrency(data.balance);
                document.getElementById('availableBalance').textContent = formatCurrency(data.availableBalance || data.balance);
                document.getElementById('lienAmount').textContent = formatCurrency(data.totalLien);

                // Handle freeze
                if (data.freezeLevel && data.freezeLevel !== 'NONE') {
                    document.getElementById('freezeWarning').style.display = 'block';
                    document.getElementById('freezeMsg').textContent = 'Account freeze level: ' + data.freezeLevel;
                    // For deposit: block if CREDIT_ONLY or FULL freeze
                    if (data.freezeLevel === 'CREDIT_ONLY' || data.freezeLevel === 'FULL') {
                        document.getElementById('submitBtn').disabled = true;
                    }
                } else {
                    document.getElementById('freezeWarning').style.display = 'none';
                }

                // Re-check holiday
                <c:if test="${isHoliday}">
                document.getElementById('submitBtn').disabled = true;
                </c:if>
            }
        }).catch(function(e) { console.error('Balance lookup failed:', e); });
}

function formatCurrency(val) {
    var num = parseFloat(val);
    return isNaN(num) ? '0.00' : num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Inline validation for amount
document.getElementById('amountInput').addEventListener('input', function() {
    var amt = parseFloat(this.value);
    var errEl = document.getElementById('amtInlineError');
    if (this.value && (isNaN(amt) || amt <= 0)) {
        errEl.classList.add('visible');
    } else {
        errEl.classList.remove('visible');
    }
});

// Form submission validation
document.getElementById('depositForm').addEventListener('submit', function(e) {
    var acct = document.getElementById('accountNumber').value;
    if (!acct) {
        e.preventDefault();
        document.getElementById('acctInlineError').classList.add('visible');
        return;
    }
    var amt = parseFloat(document.getElementById('amountInput').value);
    if (isNaN(amt) || amt <= 0) {
        e.preventDefault();
        document.getElementById('amtInlineError').classList.add('visible');
    }
});

// Auto-load balance if account number pre-filled
if (document.getElementById('accountNumber').value) {
    refreshAccountBalance();
}
</script>

<%@ include file="../layout/footer.jsp" %>
