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
                    <input type="text" class="form-control" value="<c:out value='${auditUpdatedAt}' default='--'/>" disabled/>
                    <small class="text-muted">Maker: <c:out value='${auditLastModifiedBy}' default='--'/></small>
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
                    <div id="riskBadge" class="mt-1">
                        <c:choose>
                            <c:when test="${customerDTO.riskCategory == 'HIGH'}"><span class="badge bg-danger fs-6">HIGH</span></c:when>
                            <c:when test="${customerDTO.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark fs-6">MEDIUM</span></c:when>
                            <c:otherwise><span class="badge bg-success fs-6">LOW</span></c:otherwise>
                        </c:choose>
                    </div>
                    <form:hidden path="riskCategory" id="riskCategoryHidden"/>
                    <small class="text-muted">Auto-derived from income, occupation &amp; PEP</small>
                </div>
            </div>
        </div>
    </div>

    <%-- Governance Flags --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-flag me-2"></i>Governance Flags</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Annual Income (INR)</label>
                    <form:input path="annualIncome" cssClass="form-control" id="annualIncome" placeholder="e.g. 500000"/>
                    <small class="text-muted">&lt; 10L = LOW &nbsp;|&nbsp; 10L–50L = MEDIUM &nbsp;|&nbsp; &gt;= 50L = HIGH</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Occupation</label>
                    <form:select path="occupation" cssClass="form-select" id="occupation">
                        <form:option value="" label="Select Occupation"/>
                        <form:option value="SALARIED" label="Salaried"/>
                        <form:option value="BUSINESS" label="Business"/>
                        <form:option value="SELF_EMPLOYED" label="Self Employed"/>
                        <form:option value="STUDENT" label="Student"/>
                        <form:option value="RETIRED" label="Retired"/>
                        <form:option value="GOVERNMENT_OFFICIAL" label="Government Official (HIGH RISK)"/>
                        <form:option value="POLITICIAN" label="Politician (HIGH RISK)"/>
                        <form:option value="FOREIGN_NATIONAL" label="Foreign National (HIGH RISK)"/>
                        <form:option value="ARMS" label="Arms / Defence (HIGH RISK)"/>
                        <form:option value="GAMBLING" label="Gambling (HIGH RISK)"/>
                        <form:option value="OTHER" label="Other"/>
                    </form:select>
                    <small class="text-muted">High-risk occupations auto-elevate risk to HIGH.</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label">PEP (Politically Exposed Person)</label>
                    <div class="mt-2">
                        <div class="form-check form-switch">
                            <form:checkbox path="isPep" cssClass="form-check-input" id="isPep"/>
                            <label class="form-check-label" for="isPep">
                                Mark as PEP — immediately elevates risk to <strong>HIGH</strong>
                            </label>
                        </div>
                    </div>
                    <small class="text-muted">Required per RBI KYC Master Direction.</small>
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

    <%-- KYC Warning — checker cannot approve until KYC = VERIFIED --%>
    <div class="alert alert-warning d-flex align-items-start mb-3">
        <i class="bi bi-exclamation-triangle-fill me-2 mt-1"></i>
        <div>
            <strong>KYC Reminder:</strong> After submitting this update, the record will return to
            <code>PENDING_APPROVAL</code> and KYC status will reset to <code>PENDING</code>.
            A checker <strong>cannot approve</strong> the record until KYC is set to
            <strong>VERIFIED</strong> by an authorised officer.
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

<script>
document.addEventListener("DOMContentLoaded", function() {

    const HIGH_RISK_OCCUPATIONS = [
        "POLITICIAN", "GOVERNMENT_OFFICIAL", "FOREIGN_NATIONAL", "ARMS", "GAMBLING"
    ];

    function deriveRisk() {
        const pep    = document.getElementById("isPep");
        const income = document.getElementById("annualIncome");
        const occ    = document.getElementById("occupation");
        const badge  = document.getElementById("riskBadge");
        const hidden = document.getElementById("riskCategoryHidden");

        if (!badge || !hidden) return;

        let risk = "LOW";

        if (pep && pep.checked) {
            risk = "HIGH";
        } else if (occ && HIGH_RISK_OCCUPATIONS.indexOf(occ.value) !== -1) {
            risk = "HIGH";
        } else if (income && income.value) {
            const amt = parseFloat(income.value);
            if (!isNaN(amt)) {
                if (amt >= 5000000) risk = "HIGH";
                else if (amt >= 1000000) risk = "MEDIUM";
            }
        }

        hidden.value = risk;

        const colours = { LOW: "bg-success", MEDIUM: "bg-warning text-dark", HIGH: "bg-danger" };
        badge.innerHTML =
            '<span class="badge fs-6 ' + (colours[risk] || "bg-secondary") + '">' + risk + '</span>';
    }

    const pepEl    = document.getElementById("isPep");
    const incomeEl = document.getElementById("annualIncome");
    const occEl    = document.getElementById("occupation");

    if (pepEl)    pepEl.addEventListener("change", deriveRisk);
    if (incomeEl) incomeEl.addEventListener("input", deriveRisk);
    if (occEl)    occEl.addEventListener("change", deriveRisk);

    deriveRisk();
});
</script>

<%@ include file="../layout/footer.jsp" %>
