<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-arrow-up-circle"></i> Teller Cash Withdrawal</h3>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Teller Inquiry</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/teller/withdraw" id="tellerWithdrawForm">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-wallet2 me-2"></i>Account & Amount</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-5">
                    <label for="accountNumber" class="form-label fw-bold">Customer Account Number</label>
                    <input type="text" name="accountNumber" id="accountNumber" class="form-control" required placeholder="e.g. SAV-1001-0001"/>
                </div>
                <div class="col-md-3">
                    <label for="amount" class="form-label fw-bold">Withdrawal Amount</label>
                    <input type="number" name="amount" id="amount" class="form-control form-control-lg" step="0.01" min="0.01" required readonly placeholder="Auto from denominations"/>
                </div>
                <div class="col-md-4">
                    <label for="narration" class="form-label">Narration</label>
                    <input type="text" name="narration" id="narration" class="form-control" maxlength="500" placeholder="Cash withdrawal narration"/>
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Denomination Breakdown</h5></div>
        <div class="card-body">
            <table class="table table-bordered table-sm">
                <thead class="table-light">
                    <tr><th>Denomination</th><th style="width:120px">Count</th><th style="width:160px">Total</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="dv" items="${'2000,500,200,100,50,20,10'.split(',')}">
                    <tr>
                        <td>&#8377;<c:out value="${dv}"/></td>
                        <td><input type="number" class="form-control form-control-sm denom-count" data-value="${dv}" min="0" value="0"/></td>
                        <td class="denom-total text-end fw-bold">0.00</td>
                    </tr>
                    </c:forEach>
                </tbody>
                <tfoot>
                    <tr class="table-danger"><td class="fw-bold">Grand Total</td><td></td><td class="text-end fw-bold" id="grandTotal">0.00</td></tr>
                </tfoot>
            </table>
        </div>
    </div>

    <button type="submit" class="btn btn-danger btn-lg" id="submitBtn"><i class="bi bi-arrow-up-circle"></i> Submit Withdrawal</button>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary">Cancel</a>
</form>

<script>
(function(){
    var denomInputs = document.querySelectorAll('.denom-count');
    var grandTotalEl = document.getElementById('grandTotal');
    var amountEl = document.getElementById('amount');
    var form = document.getElementById('tellerWithdrawForm');

    function recalc(){
        var total = 0;
        denomInputs.forEach(function(inp){
            var row = inp.closest('tr');
            var val = parseInt(inp.getAttribute('data-value')) || 0;
            var cnt = parseInt(inp.value) || 0;
            var lineTotal = val * cnt;
            row.querySelector('.denom-total').textContent = lineTotal.toLocaleString(undefined,{minimumFractionDigits:2});
            total += lineTotal;
        });
        grandTotalEl.textContent = total.toLocaleString(undefined,{minimumFractionDigits:2});
        amountEl.value = total.toFixed(2);
    }
    denomInputs.forEach(function(inp){ inp.addEventListener('input', recalc); });

    form.addEventListener('submit', function(e){
        var idx = 0;
        denomInputs.forEach(function(inp){
            var cnt = parseInt(inp.value) || 0;
            if(cnt > 0){
                var dv = inp.getAttribute('data-value');
                var h1 = document.createElement('input'); h1.type='hidden'; h1.name='denominations['+idx+'].denominationValue'; h1.value=dv; form.appendChild(h1);
                var h2 = document.createElement('input'); h2.type='hidden'; h2.name='denominations['+idx+'].count'; h2.value=cnt; form.appendChild(h2);
                idx++;
            }
        });
        if(idx === 0){ e.preventDefault(); alert('Please enter at least one denomination count.'); }
    });
})();
</script>

<%@ include file="../layout/footer.jsp" %>
