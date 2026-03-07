<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-journal-text"></i> Trial Balance</h3>
    <a href="${pageContext.request.contextPath}/reports" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<c:if test="${report != null}">
<div class="card shadow">
    <div class="card-header">
        <strong>Report Date:</strong> ${report.reportDate}
        <span class="float-end">
            Balanced: <c:choose>
                <c:when test="${report.balanced}"><span class="badge bg-success">YES</span></c:when>
                <c:otherwise><span class="badge bg-danger">NO</span></c:otherwise>
            </c:choose>
        </span>
    </div>
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr><th>GL Code</th><th>GL Name</th><th>Type</th><th class="text-end">Debit</th><th class="text-end">Credit</th></tr>
            </thead>
            <tbody>
                <c:forEach var="line" items="${report.lines}">
                    <tr>
                        <td><code>${line.glCode}</code></td>
                        <td>${line.glName}</td>
                        <td><span class="badge bg-info">${line.accountType}</span></td>
                        <td class="text-end">${line.debitBalance}</td>
                        <td class="text-end">${line.creditBalance}</td>
                    </tr>
                </c:forEach>
            </tbody>
            <tfoot class="table-dark">
                <tr>
                    <td colspan="3"><strong>TOTALS</strong></td>
                    <td class="text-end"><strong>${report.totalDebits}</strong></td>
                    <td class="text-end"><strong>${report.totalCredits}</strong></td>
                </tr>
            </tfoot>
        </table>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
