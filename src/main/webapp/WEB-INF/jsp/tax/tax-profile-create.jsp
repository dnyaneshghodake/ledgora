<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-receipt"></i> Tax Profile</h3>
    <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>
<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-receipt"></i> Tax Compliance Details</h5>
        <small class="text-muted">Fields marked with * are required</small>
    </div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/tax-profiles/save">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <c:if test="${not empty taxProfile.id}">
                <input type="hidden" name="id" value="${taxProfile.id}" />
            </c:if>
            <c:if test="${not empty param.customerId}">
                <input type="hidden" name="customerId" value="${param.customerId}" />
            </c:if>

            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label">Customer</label>
                    <input type="text" class="form-control" value="<c:out value="${customerName}"/>" disabled/>
                </div>

                <div class="col-md-6">
                    <label class="form-label">PAN Number *</label>
                    <input type="text" name="panNumber" class="form-control" required
                           pattern="[A-Z]{5}[0-9]{4}[A-Z]{1}" title="PAN format: ABCDE1234F"
                           maxlength="10" value="<c:out value="${taxProfile.panNumber}"/>"/>
                    <small class="text-muted">Format: ABCDE1234F</small>
                </div>

                <div class="col-md-6">
                    <label class="form-label">Aadhaar Number</label>
                    <input type="text" name="aadhaarNumber" class="form-control"
                           pattern="[0-9]{12}" title="12 digit numeric" maxlength="12"
                           value="<c:out value="${taxProfile.aadhaarMasked}"/>"/>
                    <small class="text-muted">12 digits - will be masked in display</small>
                </div>

                <div class="col-md-6">
                    <label class="form-label">GST Number</label>
                    <input type="text" name="gstNumber" class="form-control" maxlength="20"
                           value="<c:out value="${taxProfile.gstNumber}"/>"/>
                    <small class="text-muted">Mandatory for CORPORATE customers</small>
                </div>

                <div class="col-md-6">
                    <label class="form-label">Tax Residency Country</label>
                    <input type="text" name="taxResidencyCountry" class="form-control" maxlength="50"
                           value="<c:out value="${taxProfile.taxResidencyCountry}"/>"/>
                </div>

                <div class="col-md-4">
                    <div class="form-check form-switch mt-4">
                        <input class="form-check-input" type="checkbox" name="tdsApplicable" id="tdsApplicable"
                               ${taxProfile.tdsApplicable ? 'checked' : ''}/>
                        <label class="form-check-label" for="tdsApplicable">TDS Applicable</label>
                    </div>
                </div>

                <div class="col-md-4">
                    <label class="form-label">TDS Rate (%)</label>
                    <input type="number" name="tdsRate" class="form-control" step="0.01" min="0" max="100"
                           value="<c:out value="${taxProfile.tdsRate}"/>"/>
                </div>

                <div class="col-md-4">
                    <div class="form-check form-switch mt-4">
                        <input class="form-check-input" type="checkbox" name="fatcaDeclaration" id="fatcaDeclaration"
                               ${taxProfile.fatcaDeclaration ? 'checked' : ''}/>
                        <label class="form-check-label" for="fatcaDeclaration">FATCA Declaration</label>
                    </div>
                </div>

                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Save Tax Profile</button>
                    <a href="${pageContext.request.contextPath}/customers" class="btn btn-secondary ms-2">Cancel</a>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
