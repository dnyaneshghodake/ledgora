<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-plus-circle"></i> Create Account</h3>
        <hr>
    </div>
</div>

<div class="row justify-content-center">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body p-4">
                <form:form action="${pageContext.request.contextPath}/accounts/create" method="post" modelAttribute="accountDTO" id="createAccountForm">
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="accountName" class="form-label">Account Name *</label>
                            <form:input path="accountName" cssClass="form-control" id="accountName" required="true"/>
                        </div>
                        <div class="col-md-6">
                            <label for="accountType" class="form-label">Account Type *</label>
                            <form:select path="accountType" cssClass="form-select" id="accountType" required="true">
                                <option value="">Select Type</option>
                                <c:forEach var="type" items="${accountTypes}">
                                    <option value="${type}">${type}</option>
                                </c:forEach>
                            </form:select>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="customerName" class="form-label">Customer Name *</label>
                            <form:input path="customerName" cssClass="form-control" id="customerName" required="true"/>
                        </div>
                        <div class="col-md-6">
                            <label for="customerEmail" class="form-label">Customer Email</label>
                            <form:input path="customerEmail" type="email" cssClass="form-control" id="customerEmail"/>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <label for="customerPhone" class="form-label">Phone</label>
                            <form:input path="customerPhone" cssClass="form-control" id="customerPhone"/>
                        </div>
                        <div class="col-md-4">
                            <label for="branchCode" class="form-label">Branch Code</label>
                            <form:input path="branchCode" cssClass="form-control" id="branchCode"/>
                        </div>
                        <div class="col-md-4">
                            <label for="currency" class="form-label">Currency</label>
                            <form:input path="currency" cssClass="form-control" id="currency" placeholder="INR"/>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="glAccountCode" class="form-label">GL Account Code</label>
                        <form:input path="glAccountCode" cssClass="form-control" id="glAccountCode" placeholder="e.g., 2100"/>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Create Account</button>
                        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary">Cancel</a>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
