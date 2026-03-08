<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-arrow-left-right"></i> Transfer</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<c:if test="${isHoliday}">
<div class="alert alert-danger"><i class="bi bi-calendar-x"></i> <strong>HOLIDAY</strong> - Manual transactions are blocked.</div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/transactions/transfer" id="transferForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="row g-3">
                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong>From Account (Debit)</strong></div>
                        <div class="card-body">
                            <label class="form-label">Account Number *</label>
                            <div class="input-group">
                                <input type="text" name="fromAccountNumber" id="fromAccount" class="form-control" required/>
                                <button type="button" class="btn btn-outline-primary" onclick="lookupFrom()"><i class="bi bi-search"></i></button>
                            </div>
                            <div class="mt-2" id="fromInfo"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong>To Account (Credit)</strong></div>
                        <div class="card-body">
                            <label class="form-label">Account Number *</label>
                            <div class="input-group">
                                <input type="text" name="toAccountNumber" id="toAccount" class="form-control" required/>
                                <button type="button" class="btn btn-outline-primary" onclick="lookupTo()"><i class="bi bi-search"></i></button>
                            </div>
                            <div class="mt-2" id="toInfo"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <label class="form-label">Amount *</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01" id="amountInput"/>
                </div>
                <div class="col-md-6">
                    <label class="form-label">Description</label>
                    <input type="text" name="description" class="form-control" maxlength="255"/>
                </div>
                <div class="col-12"><hr>
                    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}><i class="bi bi-arrow-left-right"></i> Execute Transfer</button>
                </div>
            </div>
        </form>
    </div>
</div>

<script>
function lookupFrom() { doLookup('fromAccount', 'fromInfo'); }
function lookupTo() { doLookup('toAccount', 'toInfo'); }
function doLookup(inputId, infoId) {
    var num = document.getElementById(inputId).value;
    if (!num) return;
    fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
        .then(function(r) { return r.json(); })
        .then(function(d) {
            if (d) {
                var html = '<small><strong>' + (d.accountName||'') + '</strong> | Balance: ' + (d.balance||'0') + ' | Available: ' + (d.availableBalance||d.balance||'0') + '</small>';
                if (d.freezeLevel && d.freezeLevel !== 'NONE') html += '<br><span class="badge bg-danger">Freeze: ' + d.freezeLevel + '</span>';
                document.getElementById(infoId).innerHTML = html;
            }
        }).catch(function(e) { console.error(e); });
}
document.getElementById('transferForm').addEventListener('submit', function(e) {
    var amt = parseFloat(document.getElementById('amountInput').value);
    if (isNaN(amt) || amt <= 0) { e.preventDefault(); alert('Amount must be > 0.'); }
});
</script>

<%@ include file="../layout/footer.jsp" %>
