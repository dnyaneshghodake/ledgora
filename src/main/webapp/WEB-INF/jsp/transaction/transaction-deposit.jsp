<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-cash-stack"></i> Deposit</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Holiday Warning --%>
<c:if test="${isHoliday}">
<div class="alert alert-danger"><i class="bi bi-calendar-x"></i> <strong>HOLIDAY</strong> - Manual transactions are blocked on holidays.</div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/transactions/deposit" id="depositForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

            <div class="row g-3">
                <%-- Account Lookup (PART 5) --%>
                <div class="col-md-6">
                    <label class="form-label">Account Number *</label>
                    <div class="input-group">
                        <input type="text" name="accountNumber" id="accountNumber" class="form-control" required
                               value="<c:out value="${param.accountNumber}"/>" placeholder="Enter account number"/>
                        <button type="button" class="btn btn-outline-primary" onclick="lookupAccount()"><i class="bi bi-search"></i></button>
                    </div>
                </div>

                <%-- Account Info Display --%>
                <div class="col-md-6">
                    <label class="form-label">Account Name</label>
                    <input type="text" class="form-control" id="accountNameDisplay" disabled placeholder="Auto-filled"/>
                </div>

                <%-- Balance Display (PART 7) --%>
                <div class="col-md-3">
                    <div class="card border-primary">
                        <div class="card-body text-center p-2">
                            <small class="text-primary">Ledger Balance</small>
                            <h5 id="ledgerBalance" class="mb-0">--</h5>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-success">
                        <div class="card-body text-center p-2">
                            <small class="text-success">Available Balance</small>
                            <h5 id="availableBalance" class="mb-0">--</h5>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-warning">
                        <div class="card-body text-center p-2">
                            <small class="text-warning">Lien Amount</small>
                            <h5 id="lienAmount" class="mb-0">--</h5>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="card border-info">
                        <div class="card-body text-center p-2">
                            <small class="text-info">Business Date</small>
                            <h5 id="businessDate" class="mb-0"><c:out value="${businessDate}" default="--"/></h5>
                        </div>
                    </div>
                </div>

                <%-- Freeze Warning --%>
                <div class="col-12" id="freezeWarning" style="display:none;">
                    <div class="alert alert-danger"><i class="bi bi-slash-circle"></i> <strong>FREEZE ACTIVE</strong> - <span id="freezeMsg"></span></div>
                </div>

                <div class="col-md-6">
                    <label class="form-label">Amount *</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01"
                           placeholder="Enter amount (must be > 0)" id="amountInput"/>
                    <small class="text-muted">Must be positive. Zero and negative amounts are blocked.</small>
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
                </div>
            </div>
        </form>
    </div>
</div>

<script>
function lookupAccount() {
    var num = document.getElementById('accountNumber').value;
    if (!num) return;
    fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data) {
                document.getElementById('accountNameDisplay').value = data.accountName || '';
                document.getElementById('ledgerBalance').textContent = data.balance || '0.00';
                document.getElementById('availableBalance').textContent = data.availableBalance || data.balance || '0.00';
                document.getElementById('lienAmount').textContent = data.totalLien || '0.00';
                if (data.freezeLevel && data.freezeLevel !== 'NONE') {
                    document.getElementById('freezeWarning').style.display = 'block';
                    document.getElementById('freezeMsg').textContent = 'Account freeze level: ' + data.freezeLevel;
                    if (data.freezeLevel === 'CREDIT_ONLY' || data.freezeLevel === 'FULL') {
                        document.getElementById('submitBtn').disabled = true;
                    }
                }
            }
        }).catch(function(e) { console.error(e); });
}

document.getElementById('depositForm').addEventListener('submit', function(e) {
    var amt = parseFloat(document.getElementById('amountInput').value);
    if (isNaN(amt) || amt <= 0) {
        e.preventDefault();
        alert('Amount must be greater than zero.');
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
