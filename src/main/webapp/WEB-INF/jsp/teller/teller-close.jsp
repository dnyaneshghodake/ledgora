<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-box-arrow-right"></i> Teller Session Closure</h3>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Teller Inquiry</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/teller/close" id="tellerCloseForm">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Physical Cash Declaration</h5></div>
        <div class="card-body">
            <div class="row g-3 mb-3">
                <div class="col-md-4">
                    <label for="declaredAmount" class="form-label fw-bold">Declared Cash Amount</label>
                    <input type="number" name="declaredAmount" id="declaredAmount" class="form-control form-control-lg" step="0.01" min="0" required readonly placeholder="Auto from denominations"/>
                </div>
            </div>

            <h6 class="mb-2">Closing Denomination Breakdown</h6>
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
                    <tr class="table-warning"><td class="fw-bold">Grand Total</td><td></td><td class="text-end fw-bold" id="grandTotal">0.00</td></tr>
                </tfoot>
            </table>

            <div class="alert alert-warning mt-3"><i class="bi bi-exclamation-triangle"></i> If the declared amount does not match the system balance, a <strong>Cash Difference Log</strong> will be created. Supervisor must resolve before session can be closed.</div>
        </div>
    </div>

    <button type="submit" class="btn btn-warning btn-lg" id="submitBtn"><i class="bi bi-box-arrow-right"></i> Request Closure</button>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary">Cancel</a>
</form>

<script>
(function(){
    var denomInputs = document.querySelectorAll('.denom-count');
    var grandTotalEl = document.getElementById('grandTotal');
    var declaredEl = document.getElementById('declaredAmount');
    var form = document.getElementById('tellerCloseForm');

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
        declaredEl.value = total.toFixed(2);
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
    });
})();
</script>

<%@ include file="../layout/footer.jsp" %>
