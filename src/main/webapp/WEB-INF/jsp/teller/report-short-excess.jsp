<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-exclamation-triangle"></i> Cash Short / Excess Report</h3>
    <a href="${pageContext.request.contextPath}/teller/reports" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> All Reports</a>
</div>

<form method="get" action="${pageContext.request.contextPath}/teller/reports/short-excess" class="mb-3">
    <div class="row g-2 align-items-end">
        <div class="col-md-3">
            <label for="date" class="form-label">Business Date</label>
            <input type="date" name="date" id="date" class="form-control" value="${reportDate}"/>
        </div>
        <div class="col-md-2"><button type="submit" class="btn btn-danger"><i class="bi bi-search"></i> Filter</button></div>
    </div>
</form>

<div class="card shadow-sm">
    <div class="card-body table-responsive">
        <table class="table table-striped table-sm">
            <thead class="table-danger">
                <tr><th>Session</th><th>Teller</th><th>Branch</th><th class="text-end">Declared</th><th class="text-end">System</th><th class="text-end">Difference</th><th>Type</th><th>Resolved</th></tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${rows}">
                <tr>
                    <td><c:out value="${r.sessionId}"/></td>
                    <td><c:out value="${r.tellerName}"/></td>
                    <td><c:out value="${r.branchCode}"/></td>
                    <td class="text-end"><c:out value="${r.declaredAmount}"/></td>
                    <td class="text-end"><c:out value="${r.systemAmount}"/></td>
                    <td class="text-end fw-bold text-danger"><c:out value="${r.difference}"/></td>
                    <td><span class="badge bg-${r.type == 'SHORT' ? 'danger' : 'warning'}"><c:out value="${r.type}"/></span></td>
                    <td><c:out value="${r.resolved ? 'Yes' : 'No'}"/></td>
                </tr>
                </c:forEach>
                <c:if test="${empty rows}">
                <tr><td colspan="8" class="text-center text-muted">No cash differences found for this date.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
