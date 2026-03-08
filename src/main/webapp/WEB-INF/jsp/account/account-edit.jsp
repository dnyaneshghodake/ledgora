<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-pencil-square"></i> Edit Account</h3>
    <a href="${pageContext.request.contextPath}/accounts/${accountDTO.id}" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-body">
        <form:form method="post" action="${pageContext.request.contextPath}/accounts/${accountDTO.id}/edit" modelAttribute="accountDTO">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Account Number</label>
                    <input type="text" class="form-control" value="<c:out value="${accountDTO.accountNumber}"/>" disabled/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Account Name *</label>
                    <form:input path="accountName" cssClass="form-control" required="required" maxlength="100"/>
                    <form:errors path="accountName" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Account Type</label>
                    <input type="text" class="form-control" value="<c:out value="${accountDTO.accountType}"/>" disabled/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Interest Rate (%)</label>
                    <form:input path="interestRate" type="number" cssClass="form-control" step="0.01" min="0" max="100"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Overdraft Limit</label>
                    <form:input path="overdraftLimit" type="number" cssClass="form-control" step="0.01" min="0"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Status</label>
                    <form:select path="status" cssClass="form-select">
                        <option value="ACTIVE" ${accountDTO.status == 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                        <option value="INACTIVE" ${accountDTO.status == 'INACTIVE' ? 'selected' : ''}>INACTIVE</option>
                        <option value="SUSPENDED" ${accountDTO.status == 'SUSPENDED' ? 'selected' : ''}>SUSPENDED</option>
                        <option value="FROZEN" ${accountDTO.status == 'FROZEN' ? 'selected' : ''}>FROZEN</option>
                        <option value="CLOSED" ${accountDTO.status == 'CLOSED' ? 'selected' : ''}>CLOSED</option>
                    </form:select>
                </div>
                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Save Changes</button>
                </div>
            </div>
        </form:form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
