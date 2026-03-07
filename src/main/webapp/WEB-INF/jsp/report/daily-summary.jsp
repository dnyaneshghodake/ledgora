<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-calendar-day"></i> Daily Transaction Summary</h3>
    <a href="${pageContext.request.contextPath}/reports" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<c:if test="${report != null}">
<div class="row g-4 mb-4">
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Total Transactions</h6>
                <h3>${report.totalTransactions}</h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Deposits</h6>
                <h3 class="text-success">${report.depositCount}</h3>
                <small>${report.totalDepositAmount}</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Withdrawals</h6>
                <h3 class="text-danger">${report.withdrawalCount}</h3>
                <small>${report.totalWithdrawalAmount}</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Transfers</h6>
                <h3 class="text-primary">${report.transferCount}</h3>
                <small>${report.totalTransferAmount}</small>
            </div>
        </div>
    </div>
</div>
<div class="card shadow">
    <div class="card-body">
        <p><strong>Total Debits:</strong> ${report.totalDebits} | <strong>Total Credits:</strong> ${report.totalCredits} | <strong>Net:</strong> ${report.netAmount}</p>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
