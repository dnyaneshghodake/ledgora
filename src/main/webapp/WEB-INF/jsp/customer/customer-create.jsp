<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-person-plus"></i> Add Customer</h3>
    <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back to Customers
    </a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger">${error}</div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <form:form method="post" action="${pageContext.request.contextPath}/customers/create" modelAttribute="customerDTO">
            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label">First Name *</label>
                    <form:input path="firstName" cssClass="form-control" required="required" />
                    <form:errors path="firstName" cssClass="text-danger small" />
                </div>
                <div class="col-md-6">
                    <label class="form-label">Last Name *</label>
                    <form:input path="lastName" cssClass="form-control" required="required" />
                    <form:errors path="lastName" cssClass="text-danger small" />
                </div>
                <div class="col-md-6">
                    <label class="form-label">Date of Birth</label>
                    <form:input path="dob" type="date" cssClass="form-control" />
                </div>
                <div class="col-md-6">
                    <label class="form-label">National ID</label>
                    <form:input path="nationalId" cssClass="form-control" />
                </div>
                <div class="col-md-6">
                    <label class="form-label">Email</label>
                    <form:input path="email" type="email" cssClass="form-control" />
                </div>
                <div class="col-md-6">
                    <label class="form-label">Phone</label>
                    <form:input path="phone" cssClass="form-control" />
                </div>
                <div class="col-12">
                    <label class="form-label">Address</label>
                    <form:textarea path="address" cssClass="form-control" rows="3" />
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Create Customer</button>
                </div>
            </div>
        </form:form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
