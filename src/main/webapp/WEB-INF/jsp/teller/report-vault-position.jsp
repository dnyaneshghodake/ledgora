<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-safe"></i> Vault Position Report</h3>
    <a href="${pageContext.request.contextPath}/teller/reports" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> All Reports</a>
</div>

<div class="card shadow-sm">
    <div class="card-body table-responsive">
        <table class="table table-striped table-sm">
            <thead class="table-dark">
                <tr><th>Vault</th><th>Branch</th><th>Branch Name</th><th class="text-end">Current Balance</th><th class="text-end">Holding Limit</th><th class="text-end">Utilization %</th><th>Dual Custody</th></tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${rows}">
                <tr>
                    <td><c:out value="${r.vaultId}"/></td>
                    <td><c:out value="${r.branchCode}"/></td>
                    <td><c:out value="${r.branchName}"/></td>
                    <td class="text-end fw-bold"><c:out value="${r.currentBalance}"/></td>
                    <td class="text-end"><c:out value="${r.holdingLimit}"/></td>
                    <td class="text-end"><c:out value="${r.utilization}"/>%</td>
                    <td><c:out value="${r.dualCustody ? 'Yes' : 'No'}"/></td>
                </tr>
                </c:forEach>
                <c:if test="${empty rows}">
                <tr><td colspan="7" class="text-center text-muted">No vaults found.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
