<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-pencil"></i> Edit Account</h3>
        <hr>
    </div>
</div>

<div class="row justify-content-center">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body p-4">
                <form:form action="${pageContext.request.contextPath}/accounts/${accountDTO.id}/edit" method="post" modelAttribute="accountDTO">
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label class="form-label">Account Number</label>
                            <input type="text" class="form-control" value="${accountDTO.accountNumber}" disabled/>
                        </div>
                        <div class="col-md-6">
                            <label for="accountName" class="form-label">Account Name *</label>
                            <form:input path="accountName" cssClass="form-control" id="accountName" required="true"/>
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
                            <label for="glAccountCode" class="form-label">GL Account Code</label>
                            <form:input path="glAccountCode" cssClass="form-control" id="glAccountCode"/>
                        </div>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Update</button>
                        <a href="${pageContext.request.contextPath}/accounts/${accountDTO.id}" class="btn btn-secondary">Cancel</a>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
