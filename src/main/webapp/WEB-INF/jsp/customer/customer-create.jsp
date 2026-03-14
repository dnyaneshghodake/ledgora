<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-plus"></i> Customer Onboarding</h3>

    <a href="${pageContext.request.contextPath}/customers"
       class="btn btn-outline-secondary btn-sm">
        <i class="bi bi-arrow-left"></i> Back to Customers
    </a>
</div>

<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger">
        <c:out value="${error}" />
    </div>
</c:if>

<form:form method="post"
           action="${pageContext.request.contextPath}/customers/create"
           modelAttribute="customerDTO">

    <form:errors path="*" cssClass="alert alert-danger"/>

    <!-- ====================================================== -->
    <!-- CIF SNAPSHOT -->
    <!-- ====================================================== -->

    <div class="card shadow-sm mb-3">

        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0">
                <i class="bi bi-person-vcard me-2"></i>
                CIF Snapshot
            </h5>

            <div class="ms-auto">
                <span class="badge bg-warning text-dark">
                    PENDING APPROVAL
                </span>
            </div>
        </div>

        <div class="card-body">

            <div class="row g-3">

                <div class="col-md-3">
                    <label class="form-label">CIF Number</label>

                    <input type="text"
                           class="form-control"
                           value="AUTO"
                           disabled>

                    <small class="text-muted">
                        Generated on save
                    </small>
                </div>

                <div class="col-md-3">
                    <label class="form-label cbs-field-required">
                        Customer Type
                    </label>

                    <form:select path="customerType"
                                 cssClass="form-select"
                                 id="customerType">

                        <form:option value="" label="Select Type"/>
                        <form:option value="INDIVIDUAL" label="INDIVIDUAL"/>
                        <form:option value="CORPORATE" label="CORPORATE"/>

                    </form:select>

                    <form:errors path="customerType"
                                 cssClass="text-danger small"/>
                </div>

                <div class="col-md-3">
                    <label class="form-label">Branch</label>

                    <input type="text"
                           class="form-control"
                           disabled
                           value="${empty sessionScope.branchCode ? '--' : sessionScope.branchCode}">
                </div>

                <div class="col-md-3">
                    <label class="form-label">Risk Category</label>
                    <div id="riskBadge" class="mt-1">
                        <span class="badge bg-success fs-6">LOW</span>
                    </div>
                    <%-- Hidden field — value is set by JS based on income/occupation/PEP --%>
                    <form:hidden path="riskCategory" id="riskCategoryHidden"/>
                    <small class="text-muted">Auto-derived from income, occupation &amp; PEP</small>
                </div>

            </div>

            <div class="mt-2">
                <small class="text-muted">
                    Fields marked with
                    <span class="cbs-field-required">*</span>
                    are required. Record will be created in
                    <strong>PENDING</strong> approval status.
                </small>
            </div>

        </div>
    </div>


    <!-- ====================================================== -->
    <!-- PERSONAL DETAILS -->
    <!-- ====================================================== -->

    <div class="card shadow-sm mb-3">

        <div class="card-header bg-white">
            <h5 class="mb-0">
                <i class="bi bi-person-badge me-2"></i>
                Personal Details
            </h5>
        </div>

        <div class="card-body">

            <div class="row g-3">

                <div class="col-md-4">
                    <label class="form-label cbs-field-required">
                        First Name
                    </label>

                    <form:input path="firstName"
                                cssClass="form-control"
                                maxlength="50"/>

                    <form:errors path="firstName"
                                 cssClass="text-danger small"/>
                </div>

                <div class="col-md-4">
                    <label class="form-label cbs-field-required">
                        Last Name
                    </label>

                    <form:input path="lastName"
                                cssClass="form-control"
                                maxlength="50"/>

                    <form:errors path="lastName"
                                 cssClass="text-danger small"/>
                </div>

                <div class="col-md-4">

                    <label id="dobLabel"
                           class="form-label cbs-field-required">
                        Date of Birth
                    </label>

                    <form:input path="dob"
                                type="date"
                                cssClass="form-control"
                                id="dob"/>

                    <form:errors path="dob"
                                 cssClass="text-danger small"/>

                    <small class="text-muted">
                        INDIVIDUAL must be 18+ years.
                    </small>

                </div>

                <div class="col-md-4">

                    <label class="form-label">
                        National ID
                    </label>

                    <form:input path="nationalId"
                                cssClass="form-control"
                                maxlength="50"/>

                    <form:errors path="nationalId"
                                 cssClass="text-danger small"/>
                </div>

            </div>

        </div>
    </div>


    <!-- ====================================================== -->
    <!-- KYC -->
    <!-- ====================================================== -->

    <div class="card shadow-sm mb-3">

        <div class="card-header bg-white">
            <h5 class="mb-0">
                <i class="bi bi-shield-check me-2"></i>
                KYC & Compliance
            </h5>
        </div>

        <div class="card-body">

            <div class="row g-3">

                <div class="col-md-4">

                    <label class="form-label">
                        PAN Number
                    </label>

                    <form:input path="panNumber"
                                cssClass="form-control"
                                maxlength="10"/>

                    <form:errors path="panNumber"
                                 cssClass="text-danger small"/>

                </div>

                <div class="col-md-4">

                    <label class="form-label">
                        Aadhaar Number
                    </label>

                    <form:input path="aadhaarNumber"
                                cssClass="form-control"
                                maxlength="12"/>

                    <form:errors path="aadhaarNumber"
                                 cssClass="text-danger small"/>

                </div>

                <div class="col-md-4">

                    <label class="form-label">
                        GST Number
                    </label>

                    <form:input path="gstNumber"
                                cssClass="form-control"
                                maxlength="20"/>

                    <form:errors path="gstNumber"
                                 cssClass="text-danger small"/>

                </div>

            </div>

        </div>
    </div>


    <!-- ====================================================== -->
    <!-- CONTACT -->
    <!-- ====================================================== -->

    <div class="card shadow-sm mb-3">

        <div class="card-header bg-white">
            <h5 class="mb-0">
                <i class="bi bi-telephone me-2"></i>
                Contact Information
            </h5>
        </div>

        <div class="card-body">

            <div class="row g-3">

                <div class="col-md-4">

                    <label class="form-label cbs-field-required">
                        Mobile
                    </label>

                    <form:input path="phone"
                                cssClass="form-control"
                                maxlength="10"/>

                    <form:errors path="phone"
                                 cssClass="text-danger small"/>

                </div>

                <div class="col-md-4">

                    <label class="form-label cbs-field-required">
                        Email
                    </label>

                    <form:input path="email"
                                type="email"
                                cssClass="form-control"/>

                    <form:errors path="email"
                                 cssClass="text-danger small"/>

                </div>

                <div class="col-md-4">

                    <label class="form-label">
                        Address
                    </label>

                    <form:input path="address"
                                cssClass="form-control"
                                maxlength="500"/>

                    <form:errors path="address"
                                 cssClass="text-danger small"/>

                </div>

            </div>

        </div>
    </div>


    <!-- ====================================================== -->
    <!-- GOVERNANCE FLAGS (Risk Derivation Inputs)            -->
    <!-- ====================================================== -->

    <div class="card shadow-sm mb-3">

        <div class="card-header bg-white">
            <h5 class="mb-0">
                <i class="bi bi-flag me-2"></i>
                Governance Flags
            </h5>
        </div>

        <div class="card-body">

            <div class="row g-3">

                <div class="col-md-4">
                    <label class="form-label">Annual Income (INR)</label>
                    <form:input path="annualIncome"
                                cssClass="form-control"
                                id="annualIncome"
                                placeholder="e.g. 500000"/>
                    <small class="text-muted">
                        &lt; 10L = LOW &nbsp;|&nbsp; 10L–50L = MEDIUM &nbsp;|&nbsp; &gt;= 50L = HIGH
                    </small>
                </div>

                <div class="col-md-4">
                    <label class="form-label">Occupation</label>
                    <form:select path="occupation"
                                 cssClass="form-select"
                                 id="occupation">
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
                            <form:checkbox path="isPep"
                                           cssClass="form-check-input"
                                           id="isPep"/>
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


    <!-- ACTION BUTTONS -->

    <div class="d-flex gap-2">

        <button type="submit"
                class="btn btn-primary">

            <i class="bi bi-check-circle"></i>
            Create Customer (Pending Approval)

        </button>

        <a href="${pageContext.request.contextPath}/customers"
           class="btn btn-outline-secondary">

            Cancel

        </a>

    </div>

</form:form>


<script>

document.addEventListener("DOMContentLoaded", function() {

    // ── DOB label toggle ──
    const type = document.getElementById("customerType");
    const dobLabel = document.getElementById("dobLabel");
    if (type && dobLabel) {
        type.addEventListener("change", function() {
            dobLabel.textContent =
                this.value === "CORPORATE" ? "Incorporation Date *" : "Date of Birth *";
        });
    }

    // ── Live risk derivation ──
    const HIGH_RISK_OCCUPATIONS = [
        "POLITICIAN", "GOVERNMENT_OFFICIAL", "FOREIGN_NATIONAL", "ARMS", "GAMBLING"
    ];

    function deriveRisk() {
        const pep      = document.getElementById("isPep");
        const income   = document.getElementById("annualIncome");
        const occ      = document.getElementById("occupation");
        const badge    = document.getElementById("riskBadge");
        const hidden   = document.getElementById("riskCategoryHidden");

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

    deriveRisk(); // run once on load to initialise badge

});

</script>

<%@ include file="../layout/footer.jsp" %>