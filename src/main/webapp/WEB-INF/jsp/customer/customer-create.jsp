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
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<div class="card shadow">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-person-badge"></i> Customer Information</h5>
        <small class="text-muted">Fields marked with * are required. Customer will be created in PENDING approval status.</small>
    </div>
    <div class="card-body">
        <form:form method="post" action="${pageContext.request.contextPath}/customers/create" modelAttribute="customerDTO">
            <div class="row g-3">
                <%-- Customer Type --%>
                <div class="col-md-4">
                    <label class="form-label">Customer Type *</label>
                    <form:select path="customerType" cssClass="form-select" required="required" id="customerType">
                        <option value="INDIVIDUAL" ${customerDTO.customerType == 'INDIVIDUAL' ? 'selected' : ''}>INDIVIDUAL</option>
                        <option value="CORPORATE" ${customerDTO.customerType == 'CORPORATE' ? 'selected' : ''}>CORPORATE</option>
                    </form:select>
                </div>
                <%-- First Name --%>
                <div class="col-md-4">
                    <label class="form-label">First Name *</label>
                    <form:input path="firstName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="firstName" cssClass="text-danger small" />
                </div>
                <%-- Last Name --%>
                <div class="col-md-4">
                    <label class="form-label">Last Name *</label>
                    <form:input path="lastName" cssClass="form-control" required="required"
                                pattern="[A-Za-z .]+" title="Alphabets, spaces, and dots only" maxlength="50"/>
                    <form:errors path="lastName" cssClass="text-danger small" />
                </div>
                <%-- Date of Birth --%>
                <div class="col-md-4">
                    <label class="form-label" id="dobLabel">Date of Birth *</label>
                    <form:input path="dob" type="date" cssClass="form-control" required="required"/>
                    <small class="text-muted">Must be 18+ years for INDIVIDUAL</small>
                    <form:errors path="dob" cssClass="text-danger small" />
                </div>
                <%-- PAN Number --%>
                <div class="col-md-4">
                    <label class="form-label">PAN Number</label>
                    <form:input path="panNumber" cssClass="form-control" placeholder="ABCDE1234F"
                                pattern="[A-Z]{5}[0-9]{4}[A-Z]{1}" title="PAN format: ABCDE1234F" maxlength="10"/>
                    <form:errors path="panNumber" cssClass="text-danger small" />
                    <small class="text-muted">Mandatory for INDIVIDUAL</small>
                </div>
                <%-- Aadhaar Number --%>
                <div class="col-md-4">
                    <label class="form-label">Aadhaar Number</label>
                    <form:input path="aadhaarNumber" cssClass="form-control" placeholder="12 digit number"
                                pattern="[0-9]{12}" title="12 digit numeric" maxlength="12"/>
                    <form:errors path="aadhaarNumber" cssClass="text-danger small" />
                    <small class="text-muted">Masked in display</small>
                </div>
                <%-- National ID --%>
                <div class="col-md-4">
                    <label class="form-label">National ID</label>
                    <form:input path="nationalId" cssClass="form-control" maxlength="50"/>
                </div>
                <%-- GST Number --%>
                <div class="col-md-4">
                    <label class="form-label">GST Number</label>
                    <form:input path="gstNumber" cssClass="form-control" placeholder="GST Number" maxlength="20"/>
                    <form:errors path="gstNumber" cssClass="text-danger small" />
                    <small class="text-muted">Mandatory for CORPORATE</small>
                </div>
                <%-- Risk Category --%>
                <div class="col-md-4">
                    <label class="form-label">Risk Category</label>
                    <form:select path="riskCategory" cssClass="form-select">
                        <option value="">Select Risk Category</option>
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                    </form:select>
                </div>
                <%-- Mobile --%>
                <div class="col-md-4">
                    <label class="form-label">Mobile *</label>
                    <form:input path="phone" cssClass="form-control" required="required"
                                pattern="[0-9]{10}" title="10 digit mobile number" maxlength="10" type="tel"/>
                    <form:errors path="phone" cssClass="text-danger small" />
                </div>
                <%-- Email --%>
                <div class="col-md-4">
                    <label class="form-label">Email *</label>
                    <form:input path="email" type="email" cssClass="form-control" required="required"/>
                    <form:errors path="email" cssClass="text-danger small" />
                </div>
                <%-- Address --%>
                <div class="col-12">
                    <label class="form-label">Address</label>
                    <form:textarea path="address" cssClass="form-control" rows="2" maxlength="500"/>
                </div>
                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-check-circle"></i> Create Customer (Pending Approval)
                    </button>
                    <a href="${pageContext.request.contextPath}/customers" class="btn btn-secondary ms-2">Cancel</a>
                </div>
            </div>
        </form:form>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    var typeSelect = document.getElementById('customerType');
    var dobLabel = document.getElementById('dobLabel');
    if (typeSelect) {
        typeSelect.addEventListener('change', function() {
            dobLabel.textContent = this.value === 'CORPORATE' ? 'Incorporation Date *' : 'Date of Birth *';
        });
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
