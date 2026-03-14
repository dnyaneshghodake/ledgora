<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-workspace"></i> Teller Cash Position Report</h3>
    <a href="${pageContext.request.contextPath}/teller/reports" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> All Reports</a>
</div>

<form method="get" action="${pageContext.request.contextPath}/teller/reports/cash-position" class="mb-3">
    <div class="row g-2 align-items-end">
        <div class="col-md-3">
            <label for="date" class="form-label">Business Date</label>
            <input type="date" name="date" id="date" class="form-control" value="${reportDate}"/>
        </div>
        <div class="col-md-2"><button type="submit" class="btn btn-primary"><i class="bi bi-search"></i> Filter</button></div>
    </div>
</form>

<div class="card shadow-sm">
    <div class="card-body table-responsive">
        <table class="table table-striped table-sm">
            <thead class="table-dark">
                <tr><th>Session</th><th>Teller</th><th>Branch</th><th>State</th><th class="text-end">Opening</th><th class="text-end">Credits</th><th class="text-end">Debits</th><th class="text-end">Current</th><th>Denom</th></tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${rows}">
                <tr>
                    <td><c:out value="${r.sessionId}"/></td>
                    <td><c:out value="${r.tellerName}"/></td>
                    <td><c:out value="${r.branchCode}"/></td>
                    <td><span class="badge bg-${r.state == 'OPEN' ? 'success' : r.state == 'CLOSED' ? 'dark' : 'warning'}"><c:out value="${r.state}"/></span></td>
                    <td class="text-end"><c:out value="${r.openingBalance}"/></td>
                    <td class="text-end text-success"><c:out value="${r.totalCredit}"/></td>
                    <td class="text-end text-danger"><c:out value="${r.totalDebit}"/></td>
                    <td class="text-end fw-bold"><c:out value="${r.currentBalance}"/></td>
                    <td><a href="${pageContext.request.contextPath}/teller/reports/denomination-summary?sessionId=${r.sessionId}" class="btn btn-sm btn-outline-info"><i class="bi bi-cash-coin"></i></a></td>
                </tr>
                </c:forEach>
                <c:if test="${empty rows}">
                <tr><td colspan="9" class="text-center text-muted">No teller sessions found for this date.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
