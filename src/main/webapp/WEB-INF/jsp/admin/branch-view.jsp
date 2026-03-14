<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-geo-alt"></i> Branch (SOL) Details</h3>
    <a href="${pageContext.request.contextPath}/admin/branches" class="btn btn-outline-primary btn-sm"><i class="bi bi-arrow-left"></i> Back to Registry</a>
</div>

<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Branch Information Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-info-circle"></i> Branch Information</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-3"><strong>SOL ID:</strong> <code><c:out value="${branch.branchCode}"/></code></div>
            <div class="col-md-3"><strong>Branch Name:</strong> <c:out value="${branch.branchName}" default="${branch.name}"/></div>
            <div class="col-md-3"><strong>Branch Type:</strong> <span class="badge bg-info"><c:out value="${branch.branchType}" default="BRANCH"/></span></div>
            <div class="col-md-3"><strong>Status:</strong>
                <c:choose>
                    <c:when test="${branch.isActive}"><span class="badge bg-success">ACTIVE</span></c:when>
                    <c:otherwise><span class="badge bg-danger">INACTIVE</span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- RBI Identifiers Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-shield-check"></i> RBI Identifiers</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-4"><strong>IFSC Code:</strong> <code><c:out value="${branch.ifscCode}" default="--"/></code></div>
            <div class="col-md-4"><strong>MICR Code:</strong> <code><c:out value="${branch.micrCode}" default="--"/></code></div>
            <div class="col-md-4"><strong>Tenant:</strong> <c:out value="${branch.tenant != null ? branch.tenant.tenantName : '--'}"/></div>
        </div>
    </div>
</div>

<%-- Address & Contact Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-geo-alt-fill"></i> Address & Contact</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-6"><strong>Address:</strong> <c:out value="${branch.address}" default="--"/></div>
            <div class="col-md-3"><strong>City:</strong> <c:out value="${branch.city}" default="--"/></div>
            <div class="col-md-3"><strong>State:</strong> <c:out value="${branch.state}" default="--"/></div>
            <div class="col-md-3"><strong>Pincode:</strong> <c:out value="${branch.pincode}" default="--"/></div>
            <div class="col-md-3"><strong>Phone:</strong> <c:out value="${branch.contactPhone}" default="--"/></div>
            <div class="col-md-3"><strong>Email:</strong> <c:out value="${branch.contactEmail}" default="--"/></div>
        </div>
    </div>
</div>

<%-- Audit Info --%>
<c:set var="auditCreatedAt" value="${branch.createdAt}" scope="request"/>
<c:set var="auditUpdatedAt" value="${branch.updatedAt}" scope="request"/>
<c:set var="auditCurrentStatus" value="${branch.isActive ? 'ACTIVE' : 'INACTIVE'}" scope="request"/>
<c:set var="auditEntityType" value="Branch" scope="request"/>
<c:set var="auditEntityId" value="${branch.branchCode}" scope="request"/>
<%@ include file="../layout/audit-info.jsp" %>

<%-- Governance Note --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    IFSC and MICR codes are RBI-mandated identifiers. All branch changes are logged in the audit trail.
</div>

<%@ include file="../layout/footer.jsp" %>
