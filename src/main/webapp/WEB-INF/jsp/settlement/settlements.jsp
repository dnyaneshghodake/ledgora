<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-check2-square"></i> Settlements</h3>
    <a href="${pageContext.request.contextPath}/settlements/process" class="btn btn-primary">
        <i class="bi bi-play-circle"></i> Process Settlement
    </a>
</div>

<div class="card shadow mb-3">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/settlements" class="row g-3">
            <div class="col-md-4">
                <select name="status" class="form-select">
                    <option value="">All Statuses</option>
                    <c:forEach var="s" items="${statuses}">
                        <option value="${s}" ${selectedStatus == s.name() ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary"><i class="bi bi-filter"></i> Filter</button>
            </div>
        </form>
    </div>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>Reference</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Total Debit</th>
                    <th>Total Credit</th>
                    <th>Net Amount</th>
                    <th>Transactions</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="settlement" items="${settlements}">
                    <tr>
                        <td><code>${settlement.settlementRef}</code></td>
                        <td>${settlement.businessDate}</td>
                        <td><span class="badge bg-success">${settlement.status}</span></td>
                        <td>-</td>
                        <td>-</td>
                        <td>-</td>
                        <td>${settlement.transactionCount}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/settlements/${settlement.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty settlements}">
                    <tr><td colspan="8" class="text-center text-muted py-4">No settlements found</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
