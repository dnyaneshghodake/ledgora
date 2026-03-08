<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> Account Ownership</h3>
    <a href="${pageContext.request.contextPath}/accounts/${accountId}" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Account</a>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between">
        <h5 class="mb-0">Current Owners</h5>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty ownerships}">
                <table class="table table-hover">
                    <thead class="table-light"><tr><th>Customer</th><th>Type</th><th>Percentage</th><th>Operational</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                        <c:forEach var="o" items="${ownerships}">
                        <tr>
                            <td><c:out value="${o.customer.firstName}"/> <c:out value="${o.customer.lastName}"/></td>
                            <td><span class="badge bg-info"><c:out value="${o.ownershipType}"/></span></td>
                            <td><c:out value="${o.ownershipPercentage}"/>%</td>
                            <td><c:choose><c:when test="${o.operational}"><span class="badge bg-success">Yes</span></c:when><c:otherwise><span class="badge bg-secondary">No</span></c:otherwise></c:choose></td>
                            <td><span class="badge bg-success"><c:out value="${o.approvalStatus}"/></span></td>
                            <td>
                                <form method="post" action="${pageContext.request.contextPath}/ownerships/${o.id}/remove" style="display:inline;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                    <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Remove this ownership?')"><i class="bi bi-trash"></i></button>
                                </form>
                            </td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise><p class="text-muted text-center">No ownership records.</p></c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card shadow">
    <div class="card-header bg-white"><h5 class="mb-0">Add Ownership</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/ownerships/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="accountId" value="${accountId}" />
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">Customer ID *</label>
                    <input type="number" name="customerId" class="form-control" required/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Type *</label>
                    <select name="ownershipType" class="form-select" required>
                        <c:forEach var="ot" items="${ownershipTypes}">
                            <option value="${ot}"><c:out value="${ot}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Percentage *</label>
                    <input type="number" name="ownershipPercentage" class="form-control" required min="0" max="100" step="0.01"/>
                </div>
                <div class="col-md-3">
                    <div class="form-check form-switch mt-4">
                        <input class="form-check-input" type="checkbox" name="isOperational" id="isOperational" checked/>
                        <label class="form-check-label" for="isOperational">Operational</label>
                    </div>
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-plus-circle"></i> Add Owner</button>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
