<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-arrow-left-right"></i> Transfer</h3>
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
        <form method="post" action="${pageContext.request.contextPath}/transactions/transfer" id="transferForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="transactionType" value="TRANSFER"/>
            <div class="row g-3">
                <%-- Holiday Warning --%>
                <c:if test="${isHoliday}">
                <div class="col-12">
                    <div class="cbs-txn-holiday-warning">
                        <i class="bi bi-calendar-x"></i>
                        <span>Today is a bank holiday. Transactions are restricted.</span>
                    </div>
                </div>
                </c:if>

                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-up-circle text-danger"></i> From Account (Debit)</strong></div>
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
                            <%-- From Account Balance Cards --%>
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
                                    <div class="card border-warning cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-warning">Lien</small>
                                            <h6 id="fromLien" class="mb-0 text-warning">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="fromFreezeWarning" class="cbs-txn-freeze-warning mt-2" style="display:none;">
                                <i class="bi bi-slash-circle"></i> <span id="fromFreezeMsg"></span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-down-circle text-success"></i> To Account (Credit)</strong></div>
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
                            <%-- To Account Balance Cards --%>
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
                                    <div class="card border-warning cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-warning">Lien</small>
                                            <h6 id="toLien" class="mb-0 text-warning">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="toFreezeWarning" class="cbs-txn-freeze-warning mt-2" style="display:none;">
                                <i class="bi bi-slash-circle"></i> <span id="toFreezeMsg"></span>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Business Date --%>
                <div class="col-md-3">
                    <div class="card border-info cbs-txn-balance-card">
                        <div class="card-body p-2 text-center">
                            <small class="text-info">Business Date</small>
                            <h5 class="mb-0 text-info"><c:out value="${businessDate}" default="--"/></h5>
                        </div>
                    </div>
                </div>

                <div class="col-md-5">
                    <label class="form-label cbs-field-required">Amount</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01" id="amountInput"
                           placeholder="Enter transfer amount"/>
                    <div id="amtInlineError" class="cbs-inline-error">Amount must be greater than zero.</div>
                    <div id="balanceExceedError" class="cbs-inline-error">Amount exceeds source available balance.</div>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Description</label>
                    <input type="text" name="description" class="form-control" maxlength="255" placeholder="Transfer description"/>
                </div>
                <div class="col-12"><hr>
                    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}>
                        <i class="bi bi-arrow-left-right"></i> Execute Transfer
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
var _fromAvailBal = null;
var _fromFrozen = false;
var _toFrozen = false;

function formatCurrency(val) {
    var num = parseFloat(val);
    return isNaN(num) ? '0.00' : num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Auto-refresh balance when from/to accounts are selected via lookup modal
document.getElementById('fromAccount').addEventListener('change', function() {
    refreshBalance('fromAccount', 'fromInfo', 'fromBalanceRow', 'fromAvailBalance', 'fromLien', 'fromFreezeWarning', 'fromFreezeMsg', 'from');
});
document.getElementById('toAccount').addEventListener('change', function() {
    refreshBalance('toAccount', 'toInfo', 'toBalanceRow', 'toAvailBalance', 'toLien', 'toFreezeWarning', 'toFreezeMsg', 'to');
});

function refreshBalance(inputId, infoId, balRowId, availId, lienId, freezeWarnId, freezeMsgId, side) {
    var num = document.getElementById(inputId).value;
    if (!num) return;

    fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
        .then(function(r) { return r.json(); })
        .then(function(d) {
            if (d) {
                // Show info
                var html = '<small><strong>' + (d.accountName||'') + '</strong> | Type: ' + (d.accountType||'') + ' | Status: ' + (d.status||'') + '</small>';
                document.getElementById(infoId).innerHTML = html;

                // Show balance cards
                document.getElementById(balRowId).style.display = 'flex';
                var avail = parseFloat(d.availableBalance || d.balance) || 0;
                document.getElementById(availId).textContent = formatCurrency(avail);
                document.getElementById(lienId).textContent = formatCurrency(d.totalLien);

                if (side === 'from') _fromAvailBal = avail;

                // Freeze checks
                var freezeEl = document.getElementById(freezeWarnId);
                if (d.freezeLevel && d.freezeLevel !== 'NONE') {
                    freezeEl.style.display = 'flex';
                    document.getElementById(freezeMsgId).textContent = 'Freeze: ' + d.freezeLevel;
                    // From: block if DEBIT_ONLY or FULL; To: block if CREDIT_ONLY or FULL
                    if (side === 'from' && (d.freezeLevel === 'DEBIT_ONLY' || d.freezeLevel === 'FULL')) {
                        _fromFrozen = true;
                    } else if (side === 'to' && (d.freezeLevel === 'CREDIT_ONLY' || d.freezeLevel === 'FULL')) {
                        _toFrozen = true;
                    }
                } else {
                    freezeEl.style.display = 'none';
                    if (side === 'from') _fromFrozen = false;
                    if (side === 'to') _toFrozen = false;
                }
                updateSubmitState();
            }
        }).catch(function(e) { console.error('Balance lookup failed:', e); });
}

function updateSubmitState() {
    var btn = document.getElementById('submitBtn');
    <c:if test="${isHoliday}">
    btn.disabled = true;
    return;
    </c:if>
    btn.disabled = _fromFrozen || _toFrozen;
}

// Inline validation for amount
document.getElementById('amountInput').addEventListener('input', function() {
    var amt = parseFloat(this.value);
    var errEl = document.getElementById('amtInlineError');
    var balErr = document.getElementById('balanceExceedError');
    if (this.value && (isNaN(amt) || amt <= 0)) {
        errEl.classList.add('visible');
        balErr.classList.remove('visible');
    } else if (_fromAvailBal !== null && amt > _fromAvailBal) {
        errEl.classList.remove('visible');
        balErr.classList.add('visible');
    } else {
        errEl.classList.remove('visible');
        balErr.classList.remove('visible');
    }
});

// Form submission validation
document.getElementById('transferForm').addEventListener('submit', function(e) {
    var source = document.getElementById('fromAccount').value;
    var dest = document.getElementById('toAccount').value;
    if (!source || !dest) {
        e.preventDefault();
        showAlert('Please select both source and destination accounts.', 'danger');
        return;
    }
    if (source === dest) {
        e.preventDefault();
        showAlert('Source and destination accounts cannot be the same.', 'danger');
        return;
    }
    var amt = parseFloat(document.getElementById('amountInput').value);
    if (isNaN(amt) || amt <= 0) {
        e.preventDefault();
        document.getElementById('amtInlineError').classList.add('visible');
        return;
    }
    if (_fromAvailBal !== null && amt > _fromAvailBal) {
        e.preventDefault();
        document.getElementById('balanceExceedError').classList.add('visible');
        return;
    }
    if (_fromFrozen || _toFrozen) {
        e.preventDefault();
        showAlert('Transfer blocked due to account freeze.', 'danger');
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
