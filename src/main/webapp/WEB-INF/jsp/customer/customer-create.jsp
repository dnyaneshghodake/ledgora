<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-plus"></i> Customer Onboarding</h3>
    <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back to Customers
    </a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form:form method="post" action="${pageContext.request.contextPath}/customers/create" modelAttribute="customerDTO">

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 1: CIF SNAPSHOT                                           --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0"><i class="bi bi-person-vcard me-2"></i>CIF Snapshot</h5>
            <div class="ms-auto">
                <span class="badge bg-warning text-dark">PENDING APPROVAL</span>
            </div>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">CIF Number</label>
                    <input type="text" class="form-control" value="AUTO" disabled/>
                    <small class="text-muted">Generated on save</small>
                </div>
                <div class="col-md-3">
                    <label class="form-label cbs-field-required">Customer Type</label>
                    <form:select path="customerType" cssClass="form-select" required="required" id="customerType">
                        <option value="INDIVIDUAL" ${customerDTO.customerType == 'INDIVIDUAL' ? 'selected' : ''}>INDIVIDUAL</option>
                        <option value="CORPORATE" ${customerDTO.customerType == 'CORPORATE' ? 'selected' : ''}>CORPORATE</option>
                    </form:select>
                    <form:errors path="customerType" cssClass="text-danger small" />
                </div>
                <div class="col-md-3">
                    <label class="form-label">Branch</label>
                    <input type="text" class="form-control" value="<c:out value='${sessionScope.branchCode}' default='--'/>" disabled/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Risk Category</label>
                    <form:select path="riskCategory" cssClass="form-select">
                        <option value="">Select Risk Category</option>
                        <option value="LOW" ${customerDTO.riskCategory == 'LOW' ? 'selected' : ''}>LOW</option>
                        <option value="MEDIUM" ${customerDTO.riskCategory == 'MEDIUM' ? 'selected' : ''}>MEDIUM</option>
                        <option value="HIGH" ${customerDTO.riskCategory == 'HIGH' ? 'selected' : ''}>HIGH</option>
                    </form:select>
                </div>
            </div>
            <div class="mt-2">
                <small class="text-muted">Fields marked with <span class="cbs-field-required">*</span> are required. Record will be created in <strong>PENDING</strong> approval status.</small>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 2: PERSONAL DETAILS                                       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
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
                    <label for="dob" class="form-label cbs-field-required" id="dobLabel">Date of Birth</label>
                    <input type="date" name="dob" id="dob" class="form-control" required
                           value="<c:out value='${customerDTO.dob}'/>"/>
                    <form:errors path="dob" cssClass="text-danger small" />
                    <small class="text-muted">CBS rule: INDIVIDUAL must be 18+ years.</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">National ID</label>
                    <form:input path="nationalId" cssClass="form-control" maxlength="50"/>
                    <form:errors path="nationalId" cssClass="text-danger small" />
                </div>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 3: KYC & COMPLIANCE                                       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-shield-check me-2"></i>KYC &amp; Compliance</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">PAN Number</label>
                    <form:input path="panNumber" cssClass="form-control" placeholder="ABCDE1234F"
                                pattern="[A-Z]{5}[0-9]{4}[A-Z]{1}" title="PAN format: ABCDE1234F" maxlength="10"/>
                    <form:errors path="panNumber" cssClass="text-danger small" />
                    <small class="text-muted">Mandatory for INDIVIDUAL (policy validation enforced server-side).</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Aadhaar Number</label>
                    <form:input path="aadhaarNumber" cssClass="form-control" placeholder="12 digit number"
                                pattern="[0-9]{12}" title="12 digit numeric" maxlength="12"/>
                    <form:errors path="aadhaarNumber" cssClass="text-danger small" />
                    <small class="text-muted">Stored masked in production.</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">GST Number</label>
                    <form:input path="gstNumber" cssClass="form-control" placeholder="GST Number" maxlength="20"/>
                    <form:errors path="gstNumber" cssClass="text-danger small" />
                    <small class="text-muted">Mandatory for CORPORATE (policy validation enforced server-side).</small>
                </div>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 4: CONTACT INFORMATION                                    --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
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
                    <label class="form-label cbs-field-required">Email</label>
                    <form:input path="email" type="email" cssClass="form-control" required="required"/>
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

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 5: GOVERNANCE FLAGS                                       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-flag me-2"></i>Governance Flags</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Freeze Indicator</label>
                    <input type="text" class="form-control" value="NONE" disabled/>
                    <small class="text-muted">Freeze can be applied post-creation by authorized roles.</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">PEP Indicator</label>
                    <input type="text" class="form-control" value="NOT CAPTURED" disabled/>
                    <small class="text-muted">PEP flag available in Customer 360  view (future enhancement).</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Status</label>
                    <span class="badge bg-warning text-dark">PENDING_APPROVAL</span>
                    <div class="text-muted small mt-1">No postings allowed until checker approval.</div>
                </div>
            </div>
        </div>
    </div>

    <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">
            <i class="bi bi-check-circle"></i> Create Customer (Pending Approval)
        </button>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary">Cancel</a>
    </div>

</form:form>

<script>
document.addEventListener('DOMContentLoaded', function() {
    var typeSelect = document.getElementById('customerType');
    var dobLabel = document.getElementById('dobLabel');
    if (typeSelect && dobLabel) {
        typeSelect.addEventListener('change', function() {
            dobLabel.textContent = this.value === 'CORPORATE' ? 'Incorporation Date *' : 'Date of Birth *';
        });
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
