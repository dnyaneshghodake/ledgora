<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-plus-circle"></i> Create GL Account</h3>
    <a href="${pageContext.request.contextPath}/gl" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Main Content Section --%>
<div class="row justify-content-center">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body p-4">
                <form:form action="${pageContext.request.contextPath}/gl/create" method="post" modelAttribute="glDTO">
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <label for="glCode" class="form-label">GL Code *</label>
                            <form:input path="glCode" cssClass="form-control" id="glCode" required="true" placeholder="e.g., 1100"/>
                        </div>
                        <div class="col-md-8">
                            <label for="glName" class="form-label">GL Name *</label>
                            <form:input path="glName" cssClass="form-control" id="glName" required="true"/>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="accountType" class="form-label">Account Type *</label>
                            <form:select path="accountType" cssClass="form-select" id="accountType" required="true">
                                <option value="">Select Type</option>
                                <c:forEach var="type" items="${accountTypes}">
                                    <option value="${type}">${type}</option>
                                </c:forEach>
                            </form:select>
                        </div>
                        <div class="col-md-6">
                            <label for="parentId" class="form-label">Parent Account</label>
                            <form:select path="parentId" cssClass="form-select" id="parentId">
                                <option value="">None (Root Account)</option>
                                <c:forEach var="parent" items="${parentAccounts}">
                                    <option value="${parent.id}">${parent.glCode} - ${parent.glName}</option>
                                </c:forEach>
                            </form:select>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="normalBalance" class="form-label">Normal Balance</label>
                            <form:select path="normalBalance" cssClass="form-select" id="normalBalance">
                                <option value="DEBIT">DEBIT</option>
                                <option value="CREDIT">CREDIT</option>
                            </form:select>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="description" class="form-label">Description</label>
                        <form:textarea path="description" cssClass="form-control" id="description" rows="2"/>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Create</button>
                        <a href="${pageContext.request.contextPath}/gl" class="btn btn-secondary">Cancel</a>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
