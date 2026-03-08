<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-calendar-check"></i> Business Date Status</h3>
    <div>
        <a href="${pageContext.request.contextPath}/eod/validate" class="btn btn-outline-primary">
            <i class="bi bi-check-circle"></i> EOD Validation
        </a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Main Content Section --%>
<div class="row g-4">
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <i class="bi bi-calendar3" style="font-size: 2.5rem; color: var(--cbs-accent);"></i>
                <h6 class="text-muted mt-2">Current Business Date</h6>
                <h3 class="fw-bold">
                    <c:choose>
                        <c:when test="${not empty businessDate}"><c:out value="${businessDate}"/></c:when>
                        <c:otherwise><%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %></c:otherwise>
                    </c:choose>
                </h3>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <c:choose>
                    <c:when test="${businessDateStatus == 'OPEN'}">
                        <i class="bi bi-circle-fill" style="font-size: 2.5rem; color: #16a34a;"></i>
                        <h6 class="text-muted mt-2">Status</h6>
                        <h3 class="fw-bold text-success">OPEN</h3>
                    </c:when>
                    <c:when test="${businessDateStatus == 'DAY_CLOSING'}">
                        <i class="bi bi-circle-fill" style="font-size: 2.5rem; color: #d97706;"></i>
                        <h6 class="text-muted mt-2">Status</h6>
                        <h3 class="fw-bold text-warning">DAY CLOSING</h3>
                    </c:when>
                    <c:when test="${businessDateStatus == 'CLOSED'}">
                        <i class="bi bi-circle-fill" style="font-size: 2.5rem; color: #dc2626;"></i>
                        <h6 class="text-muted mt-2">Status</h6>
                        <h3 class="fw-bold text-danger">CLOSED</h3>
                    </c:when>
                    <c:otherwise>
                        <i class="bi bi-circle-fill" style="font-size: 2.5rem; color: #16a34a;"></i>
                        <h6 class="text-muted mt-2">Status</h6>
                        <h3 class="fw-bold text-success">OPEN</h3>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow text-center">
            <div class="card-body">
                <i class="bi bi-building" style="font-size: 2.5rem; color: #7c3aed;"></i>
                <h6 class="text-muted mt-2">Branch / Tenant</h6>
                <h3 class="fw-bold"><c:out value="${not empty branchCode ? branchCode : 'HQ'}"/></h3>
                <small class="text-muted"><c:out value="${not empty tenantName ? tenantName : 'Default Tenant'}"/></small>
            </div>
        </div>
    </div>
</div>

<div class="card shadow mt-4">
    <div class="card-header bg-white"><h5 class="mb-0">Business Day Timeline</h5></div>
    <div class="card-body">
        <div class="d-flex justify-content-between align-items-center">
            <div class="text-center flex-fill">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center ${businessDateStatus == 'OPEN' || businessDateStatus == 'DAY_CLOSING' || businessDateStatus == 'CLOSED' ? 'bg-success' : 'bg-secondary'}" style="width: 40px; height: 40px;">
                    <i class="bi bi-check-lg text-white"></i>
                </div>
                <div class="mt-1 small fw-bold">Day Opened</div>
            </div>
            <div class="flex-fill" style="height: 2px; background: ${businessDateStatus == 'DAY_CLOSING' || businessDateStatus == 'CLOSED' ? '#16a34a' : '#e2e8f0'};"></div>
            <div class="text-center flex-fill">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center ${businessDateStatus == 'DAY_CLOSING' || businessDateStatus == 'CLOSED' ? 'bg-warning' : 'bg-secondary'}" style="width: 40px; height: 40px;">
                    <i class="bi bi-hourglass-split text-white"></i>
                </div>
                <div class="mt-1 small fw-bold">Day Closing</div>
            </div>
            <div class="flex-fill" style="height: 2px; background: ${businessDateStatus == 'CLOSED' ? '#16a34a' : '#e2e8f0'};"></div>
            <div class="text-center flex-fill">
                <div class="rounded-circle d-inline-flex align-items-center justify-content-center ${businessDateStatus == 'CLOSED' ? 'bg-danger' : 'bg-secondary'}" style="width: 40px; height: 40px;">
                    <i class="bi bi-lock-fill text-white"></i>
                </div>
                <div class="mt-1 small fw-bold">Day Closed</div>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
