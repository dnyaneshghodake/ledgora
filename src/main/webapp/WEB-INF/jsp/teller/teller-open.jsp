<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-box-arrow-in-right"></i> Open Teller Session</h3>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Teller Inquiry</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/teller/open" id="tellerOpenForm">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Opening Cash Declaration</h5></div>
        <div class="card-body">
            <div class="row g-3 mb-3">
                <div class="col-md-4">
                    <label for="openingBalance" class="form-label fw-bold">Opening Balance</label>
                    <input type="number" name="openingBalance" id="openingBalance" class="form-control form-control-lg" step="0.01" min="0" required placeholder="0.00" readonly />
                </div>
            </div>

            <h6 class="mb-2">Denomination Breakdown</h6>
            <table class="table table-bordered table-sm" id="denomTable">
                <thead class="table-light">
                    <tr><th>Denomination</th><th style="width:120px">Count</th><th style="width:160px">Total</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="dv" items="${'2000,500,200,100,50,20,10'.split(',')}">
                    <tr>
                        <td><i class="bi bi-cash"></i> &#8377;<c:out value="${dv}"/></td>
                        <td><input type="number" name="denomCount_${dv}" class="form-control form-control-sm denom-count" data-value="${dv}" min="0" value="0"/></td>
                        <td class="denom-total text-end fw-bold">0.00</td>
                    </tr>
                    </c:forEach>
                </tbody>
                <tfoot>
                    <tr class="table-primary"><td class="fw-bold">Grand Total</td><td></td><td class="text-end fw-bold" id="grandTotal">0.00</td></tr>
                </tfoot>
            </table>
            <div id="denomMismatch" class="alert alert-danger d-none"><i class="bi bi-exclamation-triangle"></i> Denomination total must match the opening balance.</div>
        </div>
    </div>

    <button type="submit" class="btn btn-primary btn-lg" id="submitBtn"><i class="bi bi-box-arrow-in-right"></i> Request Open</button>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary">Cancel</a>
</form>

<script>
(function(){
    var denomInputs = document.querySelectorAll('.denom-count');
    var grandTotalEl = document.getElementById('grandTotal');
    var openingBalEl = document.getElementById('openingBalance');
    var form = document.getElementById('tellerOpenForm');

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
        openingBalEl.value = total.toFixed(2);
    }

    denomInputs.forEach(function(inp){ inp.addEventListener('input', recalc); });

    form.addEventListener('submit', function(e){
        // Build hidden denomination fields
        denomInputs.forEach(function(inp){
            var cnt = parseInt(inp.value) || 0;
            if(cnt > 0){
                var dv = inp.getAttribute('data-value');
                var h1 = document.createElement('input'); h1.type='hidden'; h1.name='denominations['+document.querySelectorAll('input[name^="denominations"]').length/2+'].denominationValue'; h1.value=dv; form.appendChild(h1);
                var h2 = document.createElement('input'); h2.type='hidden'; h2.name='denominations['+(document.querySelectorAll('input[name^="denominations"]').length-1)/2+'].count'; h2.value=cnt; form.appendChild(h2);
            }
        });
        var total = parseFloat(openingBalEl.value) || 0;
        if(total <= 0 && document.querySelectorAll('input[name^="denominations"]').length === 0){
            // Allow zero opening balance (teller starts with no cash)
        }
    });
})();
</script>

<%@ include file="../layout/footer.jsp" %>
