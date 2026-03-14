<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-coin"></i> Denomination Summary &mdash; Session #<c:out value="${sessionId}"/></h3>
    <a href="${pageContext.request.contextPath}/teller/reports/cash-position" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Cash Position</a>
</div>

<div class="card shadow-sm">
    <div class="card-body table-responsive">
        <table class="table table-bordered table-sm">
            <thead class="table-success">
                <tr><th>Denomination (&#8377;)</th><th class="text-end">Total Notes</th><th class="text-end">Total Amount (&#8377;)</th></tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${rows}">
                <tr>
                    <td><i class="bi bi-cash"></i> &#8377;<c:out value="${r.denomination}"/></td>
                    <td class="text-end"><c:out value="${r.totalCount}"/></td>
                    <td class="text-end fw-bold"><c:out value="${r.totalAmount}"/></td>
                </tr>
                </c:forEach>
                <c:if test="${empty rows}">
                <tr><td colspan="3" class="text-center text-muted">No denomination data for this session.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
