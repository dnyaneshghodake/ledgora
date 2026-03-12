<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-pencil-square"></i> Customer Maintenance</h3>
    <div class="d-flex gap-2">
        <a href="${pageContext.request.contextPath}/customers/${customerDTO.customerId}" class="btn btn-outline-secondary btn-sm">
            <i class="bi bi-arrow-left"></i> Back
        </a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form:form method="post" action="${pageContext.request.contextPath}/customers/${customerDTO.customerId}/edit" modelAttribute="customerDTO">

    <%-- CIF Snapshot --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0"><i class="bi bi-person-vcard me-2"></i>CIF Snapshot</h5>
            <div class="ms-auto">
                <span class="badge bg-secondary">EDIT MODE</span>
            </div>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">CIF Number</label>
                    <input type="text" class="form-control" value="<c:out value='${customerDTO.customerId}'/>" disabled/>
                    <small class="text-muted">Immutable</small>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Customer Type</label>
                    <form:select path="customerType" cssClass="form-select">
                        <option value="INDIVIDUAL" ${customerDTO.customerType == 'INDIVIDUAL' ? 'selected' : ''}>INDIVIDUAL</option>
                        <option value="CORPORATE" ${customerDTO.customerType == 'CORPORATE' ? 'selected' : ''}>CORPORATE</option>
                    </form:select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">KYC Status</label>
                    <input type="text" class="form-control" value="<c:out value='${customerDTO.kycStatus}'/>" disabled/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Last Updated</label>
                    <input type="text" class="form-control" value="<c:out value='${requestScope.auditUpdatedAt}' default='--'/>" disabled/>
                    <small class="text-muted">Last updated by: <c:out value='${requestScope.auditLastModifiedBy}' default='--'/></small>
                </div>
            </div>
        </div>
    </div>

    <%-- Personal Details --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-person-badge me-2"></i>Personal Details</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">First Name</label>
                    <form:input path="firstName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="firstName" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Last Name</label>
                    <form:input path="lastName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="lastName" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label for="dob" class="form-label">Date of Birth</label>
                    <input type="date" name="dob" id="dob" class="form-control"
                           value="<c:out value='${customerDTO.dob}'/>"/>
                    <form:errors path="dob" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">National ID</label>
                    <form:input path="nationalId" cssClass="form-control" maxlength="50"/>
                    <form:errors path="nationalId" cssClass="text-danger small" />
                </div>
            </div>
        </div>
    </div>

    <%-- KYC & Compliance --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-shield-check me-2"></i>KYC &amp; Compliance</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">PAN Number</label>
                    <input type="text" class="form-control" value="<c:out value='${customerDTO.panNumber}'/>" disabled/>
                    <small class="text-muted">Immutable</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">GST Number</label>
                    <form:input path="gstNumber" cssClass="form-control" maxlength="20"/>
                    <form:errors path="gstNumber" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Risk Category</label>
                    <form:select path="riskCategory" cssClass="form-select">
                        <option value="">Select</option>
                        <option value="LOW" ${customerDTO.riskCategory == 'LOW' ? 'selected' : ''}>LOW</option>
                        <option value="MEDIUM" ${customerDTO.riskCategory == 'MEDIUM' ? 'selected' : ''}>MEDIUM</option>
                        <option value="HIGH" ${customerDTO.riskCategory == 'HIGH' ? 'selected' : ''}>HIGH</option>
                    </form:select>
                </div>
            </div>
        </div>
    </div>

    <%-- Contact Information --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-telephone me-2"></i>Contact Information</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Mobile</label>
                    <form:input path="phone" cssClass="form-control" required="required"
                                pattern="[0-9]{10}" title="10 digit mobile number" maxlength="10" type="tel"/>
                    <form:errors path="phone" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Email</label>
                    <form:input path="email" type="email" cssClass="form-control"/>
                    <form:errors path="email" cssClass="text-danger small" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Address</label>
                    <form:input path="address" cssClass="form-control" maxlength="500"/>
                    <form:errors path="address" cssClass="text-danger small" />
                </div>
            </div>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-body">
            <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Submit for Re-Approval</button>
            <small class="text-muted ms-2"><i class="bi bi-info-circle"></i> Changes will be submitted for checker approval before taking effect.</small>
            <a href="${pageContext.request.contextPath}/customers/${customerDTO.customerId}" class="btn btn-outline-secondary ms-2">Cancel</a>
        </div>
    </div>

</form:form>

<%@ include file="../layout/footer.jsp" %>
