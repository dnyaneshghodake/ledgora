<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-coin"></i> Withdrawal</h3>
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
        <form method="post" action="${pageContext.request.contextPath}/transactions/withdraw" id="withdrawForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="transactionType" value="WITHDRAWAL"/>
            <div class="row g-3">
                <div class="col-md-6">
                    <label for="accountNumber" class="form-label cbs-field-required">Account Number</label>
                    <div class="input-group">
                        <input type="text" name="sourceAccountNumber" id="accountNumber" class="form-control" required readonly
                               value="<c:out value="${param.accountNumber}"/>" placeholder="Use lookup to select account"/>
                        <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('accountNumber','accountNameDisplay')" title="Search Account">
                            <i class="bi bi-search"></i>
                        </button>
                    </div>
                    <div id="acctInlineError" class="cbs-inline-error">Please select an account using the lookup.</div>
                </div>
                <div class="col-md-6">
                    <label for="accountNameDisplay" class="form-label">Account Name</label>
                    <input type="text" class="form-control" id="accountNameDisplay" disabled aria-disabled="true" placeholder="Auto-filled on selection"/>
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
                <div class="col-12 d-none" id="freezeWarning">
                    <div class="cbs-txn-freeze-warning">
                        <i class="bi bi-slash-circle"></i>
                        <strong>FREEZE ACTIVE</strong> &mdash; <span id="freezeMsg"></span>
                    </div>
                </div>

                <div class="col-md-6">
                    <label for="amountInput" class="form-label cbs-field-required">Amount</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01" id="amountInput"/>
                    <small class="text-muted">Cannot exceed available balance.</small>
                    <div id="amtInlineError" class="cbs-inline-error">Amount must be greater than zero.</div>
                    <div id="balanceExceedError" class="cbs-inline-error">Amount exceeds available balance.</div>
                </div>
                <div class="col-md-6">
                    <label for="descriptionInput" class="form-label">Description</label>
                    <input type="text" name="description" id="descriptionInput" class="form-control" maxlength="255" placeholder="Transaction description"/>
                </div>
                <div class="col-12"><hr>
                    <button type="submit" class="btn btn-warning btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}><i class="bi bi-cash-coin"></i> Submit Withdrawal</button>
                    <c:if test="${isHoliday}">
                        <small class="text-danger ms-3"><i class="bi bi-info-circle"></i> Submissions disabled on holidays.</small>
                    </c:if>
                </div>
            </div>
        </form>
    </div>
</div>

<script>
var _availBal = null;

// Auto-refresh balance when account is selected via lookup modal
document.getElementById('accountNumber').addEventListener('change', function() {
    refreshAccountBalance();
});

function refreshAccountBalance() {
    var num = document.getElementById('accountNumber').value;
    if (!num) return;

    // Reset previous state
    document.getElementById('freezeWarning').classList.add('d-none');
    document.getElementById('submitBtn').disabled = false;

    fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data) {
                document.getElementById('accountNameDisplay').value = data.accountName || '';
                document.getElementById('ledgerBalance').textContent = formatCurrency(data.balance);
                _availBal = parseFloat(data.availableBalance || data.balance) || 0;
                document.getElementById('availableBalance').textContent = formatCurrency(_availBal);
                document.getElementById('lienAmount').textContent = formatCurrency(data.totalLien);

                // Handle freeze - for withdrawal: block if DEBIT_ONLY or FULL freeze
                if (data.freezeLevel && data.freezeLevel !== 'NONE') {
                    document.getElementById('freezeWarning').classList.remove('d-none');
                    document.getElementById('freezeMsg').textContent = 'Account freeze level: ' + data.freezeLevel;
                    if (data.freezeLevel === 'DEBIT_ONLY' || data.freezeLevel === 'FULL') {
                        document.getElementById('submitBtn').disabled = true;
                    }
                } else {
                    document.getElementById('freezeWarning').classList.add('d-none');
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
    var balErr = document.getElementById('balanceExceedError');
    if (this.value && (isNaN(amt) || amt <= 0)) {
        errEl.classList.add('visible');
        balErr.classList.remove('visible');
    } else if (_availBal !== null && amt > _availBal) {
        errEl.classList.remove('visible');
        balErr.classList.add('visible');
    } else {
        errEl.classList.remove('visible');
        balErr.classList.remove('visible');
    }
});

// Form submission validation
document.getElementById('withdrawForm').addEventListener('submit', function(e) {
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
        return;
    }
    if (_availBal !== null && amt > _availBal) {
        e.preventDefault();
        document.getElementById('balanceExceedError').classList.add('visible');
    }
});

// Auto-load balance if account number pre-filled
if (document.getElementById('accountNumber').value) {
    refreshAccountBalance();
}
</script>

<%@ include file="../layout/footer.jsp" %>
