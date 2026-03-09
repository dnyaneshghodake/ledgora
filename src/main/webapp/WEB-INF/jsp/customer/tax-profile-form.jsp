<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-receipt"></i> <c:choose><c:when test="${isEdit}">Edit</c:when><c:otherwise>Create</c:otherwise></c:choose> Tax Profile</h3>
    <a href="${pageContext.request.contextPath}/customers/${customer.customerId}" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Customer</a>
</div>

<div class="card shadow">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-person-badge"></i> Customer: <c:out value="${customer.firstName}"/> <c:out value="${customer.lastName}"/> (ID: <c:out value="${customer.customerId}"/>)</h5>
    </div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/tax-profiles/save">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="customerId" value="${customer.customerId}" />

            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">PAN Number</label>
                    <input type="text" name="panNumber" class="form-control" maxlength="20"
                           value="<c:out value='${taxProfile.panNumber}'/>" placeholder="e.g. ABCDE1234F"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Aadhaar Number</label>
                    <input type="text" name="aadhaarNumber" class="form-control" maxlength="12"
                           value="<c:out value='${taxProfile.aadhaarNumber}'/>" placeholder="12-digit Aadhaar"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">GST Number</label>
                    <input type="text" name="gstNumber" class="form-control" maxlength="20"
                           value="<c:out value='${taxProfile.gstNumber}'/>" placeholder="e.g. 29ABCDE1234F1ZA"/>
                </div>

                <div class="col-md-3">
                    <label class="form-label">TDS Applicable</label>
                    <select name="tdsApplicable" class="form-select">
                        <option value="true" ${taxProfile.tdsApplicable ? 'selected' : ''}>Yes</option>
                        <option value="false" ${!taxProfile.tdsApplicable ? 'selected' : ''}>No</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label">TDS Rate (%)</label>
                    <input type="number" name="tdsRate" class="form-control" step="0.01" min="0" max="100"
                           value="<c:out value='${taxProfile.tdsRate}' default='0'/>"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Tax Residency</label>
                    <select name="taxResidencyStatus" class="form-select">
                        <option value="RESIDENT" ${taxProfile.taxResidencyStatus == 'RESIDENT' ? 'selected' : ''}>Resident</option>
                        <option value="NON_RESIDENT" ${taxProfile.taxResidencyStatus == 'NON_RESIDENT' ? 'selected' : ''}>Non-Resident</option>
                        <option value="NRI" ${taxProfile.taxResidencyStatus == 'NRI' ? 'selected' : ''}>NRI</option>
                    </select>
                </div>

                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-check-circle"></i> <c:choose><c:when test="${isEdit}">Update</c:when><c:otherwise>Create</c:otherwise></c:choose> Tax Profile
                    </button>
                    <a href="${pageContext.request.contextPath}/customers/${customer.customerId}" class="btn btn-secondary ms-2">Cancel</a>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
