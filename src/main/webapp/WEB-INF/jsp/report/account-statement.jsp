<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-file-earmark-text"></i> Account Statement</h3>
    <a href="${pageContext.request.contextPath}/reports" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<div class="card shadow mb-4">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/reports/account-statement" class="row g-3">
            <div class="col-md-4">
                <label class="form-label">Account Number</label>
                <input type="text" name="accountNumber" class="form-control" required value="${report != null ? report.accountNumber : ''}" />
            </div>
            <div class="col-md-3">
                <label class="form-label">Start Date</label>
                <input type="date" name="startDate" class="form-control" />
            </div>
            <div class="col-md-3">
                <label class="form-label">End Date</label>
                <input type="date" name="endDate" class="form-control" />
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100">Generate</button>
            </div>
        </form>
    </div>
</div>

<c:if test="${report != null}">
<div class="card shadow">
    <div class="card-header">
        <strong>${report.accountName}</strong> (${report.accountNumber}) | Currency: ${report.currency}
    </div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>Date</th><th>Reference</th><th>Description</th><th class="text-end">Debit</th><th class="text-end">Credit</th><th class="text-end">Balance</th></tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${report.entries}">
                    <tr>
                        <td>${entry.date}</td>
                        <td><code>${entry.transactionRef}</code></td>
                        <td>${entry.description}</td>
                        <td class="text-end">${entry.debitAmount}</td>
                        <td class="text-end">${entry.creditAmount}</td>
                        <td class="text-end fw-bold">${entry.balance}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
