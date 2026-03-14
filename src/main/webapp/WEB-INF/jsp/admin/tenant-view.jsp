<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-building"></i> Tenant Details</h3>
    <a href="${pageContext.request.contextPath}/admin/tenants" class="btn btn-outline-primary btn-sm"><i class="bi bi-arrow-left"></i> Back to Registry</a>
</div>

<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Tenant Information Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-info-circle"></i> Tenant Information</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-4"><strong>Tenant Code:</strong> <code><c:out value="${tenant.tenantCode}"/></code></div>
            <div class="col-md-4"><strong>Tenant Name:</strong> <c:out value="${tenant.tenantName}"/></div>
            <div class="col-md-4"><strong>Status:</strong>
                <c:choose>
                    <c:when test="${tenant.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                    <c:when test="${tenant.status == 'INITIALIZING'}"><span class="badge bg-info">INITIALIZING</span></c:when>
                    <c:when test="${tenant.status == 'INACTIVE'}"><span class="badge bg-danger">INACTIVE</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${tenant.status}"/></span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- Business Operations Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-calendar3"></i> Business Operations</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-3"><strong>Business Date:</strong> <c:out value="${tenant.currentBusinessDate}"/></div>
            <div class="col-md-3"><strong>Day Status:</strong>
                <c:choose>
                    <c:when test="${tenant.dayStatus == 'OPEN'}"><span class="badge bg-success">OPEN</span></c:when>
                    <c:when test="${tenant.dayStatus == 'DAY_CLOSING'}"><span class="badge bg-warning text-dark">DAY_CLOSING</span></c:when>
                    <c:when test="${tenant.dayStatus == 'CLOSED'}"><span class="badge bg-secondary">CLOSED</span></c:when>
                    <c:otherwise><span class="badge bg-danger"><c:out value="${tenant.dayStatus}"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-3"><strong>EOD Status:</strong>
                <c:choose>
                    <c:when test="${tenant.eodStatus == 'COMPLETED'}"><span class="badge bg-success">COMPLETED</span></c:when>
                    <c:when test="${tenant.eodStatus == 'IN_PROGRESS'}"><span class="badge bg-warning text-dark">IN_PROGRESS</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${tenant.eodStatus}" default="NOT_STARTED"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-3"><strong>Multi-Branch:</strong>
                <c:choose>
                    <c:when test="${tenant.multiBranchEnabled}"><span class="badge bg-success">Enabled</span></c:when>
                    <c:otherwise><span class="badge bg-secondary">Disabled</span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- RBI Regulatory Card --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-shield-check"></i> RBI Regulatory Information</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-3"><strong>Regulatory Code:</strong> <code><c:out value="${tenant.regulatoryCode}" default="--"/></code></div>
            <div class="col-md-3"><strong>Base Currency:</strong> <c:out value="${tenant.baseCurrency}" default="INR"/></div>
            <div class="col-md-3"><strong>Country:</strong> <c:out value="${tenant.country}" default="IN"/></div>
            <div class="col-md-3"><strong>Timezone:</strong> <c:out value="${tenant.timezone}" default="Asia/Kolkata"/></div>
            <div class="col-md-3"><strong>Effective From:</strong> <c:out value="${tenant.effectiveFrom}" default="--"/></div>
            <div class="col-md-9"><strong>Remarks:</strong> <c:out value="${tenant.remarks}" default="--"/></div>
        </div>
    </div>
</div>

<%-- Audit Info --%>
<c:set var="auditCreatedAt" value="${tenant.createdAt}" scope="request"/>
<c:set var="auditUpdatedAt" value="${tenant.updatedAt}" scope="request"/>
<c:set var="auditCurrentStatus" value="${tenant.status}" scope="request"/>
<c:set var="auditEntityType" value="Tenant" scope="request"/>
<c:set var="auditEntityId" value="${tenant.tenantCode}" scope="request"/>
<%@ include file="../layout/audit-info.jsp" %>

<%-- Governance Note --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All tenant configuration changes are logged in the audit trail.
</div>

<%@ include file="../layout/footer.jsp" %>
