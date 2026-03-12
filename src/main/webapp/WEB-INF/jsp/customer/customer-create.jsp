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

                    <form:select path="riskCategory"
                                 cssClass="form-select">

                        <form:option value="" label="Select Risk Category"/>
                        <form:option value="LOW" label="LOW"/>
                        <form:option value="MEDIUM" label="MEDIUM"/>
                        <form:option value="HIGH" label="HIGH"/>

                    </form:select>

                    <form:errors path="riskCategory"
                                 cssClass="text-danger small"/>
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

    const type = document.getElementById("customerType");
    const label = document.getElementById("dobLabel");

    if(type && label){

        type.addEventListener("change", function(){

            label.textContent =
                this.value === "CORPORATE"
                ? "Incorporation Date *"
                : "Date of Birth *";

        });

    }

});

</script>

<%@ include file="../layout/footer.jsp" %>