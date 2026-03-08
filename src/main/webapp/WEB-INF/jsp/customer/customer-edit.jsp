<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-pencil-square"></i> Edit Customer</h3>
    <a href="${pageContext.request.contextPath}/customers/${customerDTO.customerId}" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back
    </a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-pencil"></i> Edit Customer Details</h5>
    </div>
    <div class="card-body">
        <form:form method="post" action="${pageContext.request.contextPath}/customers/${customerDTO.customerId}/edit" modelAttribute="customerDTO">
            <div class="row g-3">
                <%-- Customer ID (read-only) --%>
                <div class="col-md-4">
                    <label class="form-label">Customer ID</label>
                    <input type="text" class="form-control" value="<c:out value='${customerDTO.customerId}'/>" disabled/>
                </div>
                <%-- Customer Type --%>
                <div class="col-md-4">
                    <label class="form-label">Customer Type</label>
                    <form:select path="customerType" cssClass="form-select">
                        <option value="INDIVIDUAL" ${customerDTO.customerType == 'INDIVIDUAL' ? 'selected' : ''}>INDIVIDUAL</option>
                        <option value="CORPORATE" ${customerDTO.customerType == 'CORPORATE' ? 'selected' : ''}>CORPORATE</option>
                    </form:select>
                </div>
                <%-- KYC Status (read-only) --%>
                <div class="col-md-4">
                    <label class="form-label">KYC Status</label>
                    <input type="text" class="form-control" value="<c:out value='${customerDTO.kycStatus}'/>" disabled/>
                </div>
                <%-- First Name --%>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">First Name</label>
                    <form:input path="firstName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="firstName" cssClass="text-danger small" />
                </div>
                <%-- Last Name --%>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Last Name</label>
                    <form:input path="lastName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="lastName" cssClass="text-danger small" />
                </div>
                <%-- Date of Birth --%>
                <div class="col-md-4">
                    <label class="form-label">Date of Birth</label>
                    <form:input path="dob" type="date" cssClass="form-control"/>
                </div>
                <%-- National ID --%>
                <div class="col-md-4">
                    <label class="form-label">National ID</label>
                    <form:input path="nationalId" cssClass="form-control" maxlength="50"/>
                </div>
                <%-- PAN Number --%>
                <div class="col-md-4">
                    <label class="form-label">PAN Number</label>
                    <form:input path="panNumber" cssClass="form-control" placeholder="ABCDE1234F"
                                pattern="[A-Z]{5}[0-9]{4}[A-Z]{1}" title="PAN format: ABCDE1234F" maxlength="10"/>
                    <form:errors path="panNumber" cssClass="text-danger small" />
                </div>
                <%-- GST Number --%>
                <div class="col-md-4">
                    <label class="form-label">GST Number</label>
                    <form:input path="gstNumber" cssClass="form-control" maxlength="20"/>
                </div>
                <%-- Risk Category --%>
                <div class="col-md-4">
                    <label class="form-label">Risk Category</label>
                    <form:select path="riskCategory" cssClass="form-select">
                        <option value="">Select</option>
                        <option value="LOW" ${customerDTO.riskCategory == 'LOW' ? 'selected' : ''}>LOW</option>
                        <option value="MEDIUM" ${customerDTO.riskCategory == 'MEDIUM' ? 'selected' : ''}>MEDIUM</option>
                        <option value="HIGH" ${customerDTO.riskCategory == 'HIGH' ? 'selected' : ''}>HIGH</option>
                    </form:select>
                </div>
                <%-- Mobile --%>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Mobile</label>
                    <form:input path="phone" cssClass="form-control" required="required"
                                pattern="[0-9]{10}" title="10 digit mobile number" maxlength="10" type="tel"/>
                    <form:errors path="phone" cssClass="text-danger small" />
                </div>
                <%-- Email --%>
                <div class="col-md-4">
                    <label class="form-label">Email</label>
                    <form:input path="email" type="email" cssClass="form-control"/>
                    <form:errors path="email" cssClass="text-danger small" />
                </div>
                <%-- Address --%>
                <div class="col-12">
                    <label class="form-label">Address</label>
                    <form:textarea path="address" cssClass="form-control" rows="2" maxlength="500"/>
                </div>
                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Save Changes</button>
                    <a href="${pageContext.request.contextPath}/customers/${customerDTO.customerId}" class="btn btn-secondary ms-2">Cancel</a>
                </div>
            </div>
        </form:form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
