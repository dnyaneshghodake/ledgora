<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="mb-4">
    <h3><i class="bi bi-bar-chart"></i> Financial Reports</h3>
</div>

<div class="row g-4">
    <div class="col-md-6">
        <div class="card shadow h-100">
            <div class="card-body">
                <h5 class="card-title"><i class="bi bi-journal-text"></i> Trial Balance</h5>
                <p class="card-text">View the trial balance report generated from ledger entries.</p>
                <a href="${pageContext.request.contextPath}/reports/trial-balance" class="btn btn-primary">Generate</a>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow h-100">
            <div class="card-body">
                <h5 class="card-title"><i class="bi bi-file-earmark-text"></i> Account Statement</h5>
                <p class="card-text">View account statement for a specific account.</p>
                <a href="${pageContext.request.contextPath}/reports/account-statement" class="btn btn-primary">Generate</a>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow h-100">
            <div class="card-body">
                <h5 class="card-title"><i class="bi bi-calendar-day"></i> Daily Summary</h5>
                <p class="card-text">Daily transaction summary with debit/credit totals.</p>
                <a href="${pageContext.request.contextPath}/reports/daily-summary" class="btn btn-primary">Generate</a>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow h-100">
            <div class="card-body">
                <h5 class="card-title"><i class="bi bi-droplet"></i> Liquidity Report</h5>
                <p class="card-text">Cash position and liquidity ratio analysis.</p>
                <a href="${pageContext.request.contextPath}/reports/liquidity" class="btn btn-primary">Generate</a>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
