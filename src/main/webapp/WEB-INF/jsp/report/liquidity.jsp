<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-droplet"></i> Liquidity Report</h3>
    <a href="${pageContext.request.contextPath}/reports" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<c:if test="${report != null}">
<div class="row g-4 mb-4">
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Total Assets</h6>
                <h3 class="text-success">${report.totalAssets}</h3>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Total Liabilities</h6>
                <h3 class="text-danger">${report.totalLiabilities}</h3>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Liquidity Ratio</h6>
                <h3>${report.liquidityRatio}</h3>
            </div>
        </div>
    </div>
</div>
<div class="card shadow">
    <div class="card-body">
        <p><strong>Net Liquidity:</strong> ${report.netLiquidity} | <strong>Cash Holdings:</strong> ${report.totalCashHoldings} | <strong>Customer Deposits:</strong> ${report.totalCustomerDeposits}</p>
    </div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>GL Code</th><th>GL Name</th><th>Type</th><th class="text-end">Balance</th></tr>
            </thead>
            <tbody>
                <c:forEach var="line" items="${report.details}">
                    <tr>
                        <td><code>${line.glCode}</code></td>
                        <td>${line.glName}</td>
                        <td><span class="badge bg-info">${line.accountType}</span></td>
                        <td class="text-end fw-bold">${line.balance}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
