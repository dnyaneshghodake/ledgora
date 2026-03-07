<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-diagram-3"></i> General Ledger - Chart of Accounts</h3>
    <a href="${pageContext.request.contextPath}/gl/create" class="btn btn-primary">
        <i class="bi bi-plus-circle"></i> Create GL Account
    </a>
</div>

<div class="card shadow">
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                    <tr>
                        <th>GL Code</th>
                        <th>Name</th>
                        <th>Type</th>
                        <th>Level</th>
                        <th>Normal Balance</th>
                        <th>Balance</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="gl" items="${allAccounts}">
                        <tr>
                            <td>
                                <c:forEach begin="1" end="${gl.level}"><span class="ms-3"></span></c:forEach>
                                <c:if test="${gl.level > 0}"><i class="bi bi-arrow-return-right text-muted"></i></c:if>
                                <code>${gl.glCode}</code>
                            </td>
                            <td>
                                <c:forEach begin="1" end="${gl.level}"><span class="ms-2"></span></c:forEach>
                                <strong>${gl.glName}</strong>
                            </td>
                            <td><span class="badge bg-info">${gl.accountType}</span></td>
                            <td>${gl.level}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${gl.normalBalance == 'DEBIT'}"><span class="badge bg-danger">DEBIT</span></c:when>
                                    <c:when test="${gl.normalBalance == 'CREDIT'}"><span class="badge bg-success">CREDIT</span></c:when>
                                </c:choose>
                            </td>
                            <td class="fw-bold">${gl.balance}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${gl.isActive}"><span class="badge bg-success">Active</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <a href="${pageContext.request.contextPath}/gl/${gl.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a>
                                <a href="${pageContext.request.contextPath}/gl/${gl.id}/edit" class="btn btn-sm btn-outline-secondary"><i class="bi bi-pencil"></i></a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty allAccounts}">
                        <tr><td colspan="8" class="text-center text-muted py-4">No GL accounts found</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
