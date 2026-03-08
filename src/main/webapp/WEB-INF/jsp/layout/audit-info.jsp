<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Audit Info Fragment
  Displays standardized audit trail section for entity view pages.

  Usage: <%@ include file="../layout/audit-info.jsp" %>

  Expected model attributes (set by controller):
    auditCreatedBy       - String, username who created the entity
    auditCreatedAt       - String/Date, creation timestamp
    auditUpdatedAt       - String/Date, last update timestamp
    auditApprovedBy      - String, username who approved
    auditApprovedAt      - String/Date, approval timestamp
    auditApprovalStatus  - String, PENDING/APPROVED/REJECTED
    auditEntityType      - String, entity type label (e.g., "Customer", "Account")
    auditEntityId        - String/Number, entity identifier
    auditTenantName      - String, tenant name
    auditBranchCode      - String, branch code
--%>

<div class="card shadow mt-4 cbs-audit-info">
    <div class="card-header bg-light">
        <h6 class="mb-0">
            <i class="bi bi-shield-lock"></i> Audit Information
            <c:if test="${not empty auditEntityType}">
                <small class="text-muted ms-2">
                    <c:out value="${auditEntityType}"/>
                    <c:if test="${not empty auditEntityId}"> #<c:out value="${auditEntityId}"/></c:if>
                </small>
            </c:if>
        </h6>
    </div>
    <div class="card-body">
        <div class="row g-3">
            <%-- Created By --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Created By</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditCreatedBy}"><i class="bi bi-person"></i> <c:out value="${auditCreatedBy}"/></c:when>
                            <c:otherwise><span class="text-muted">System</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Created At --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Created At</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditCreatedAt}"><i class="bi bi-clock"></i> <c:out value="${auditCreatedAt}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Approved By --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Approved By</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditApprovedBy}"><i class="bi bi-person-check"></i> <c:out value="${auditApprovedBy}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Approval Status --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Approval Status</small>
                    <c:choose>
                        <c:when test="${auditApprovalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                        <c:when test="${auditApprovalStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:when test="${auditApprovalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${auditApprovalStatus}" default="--"/></span></c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Updated At --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Last Updated</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditUpdatedAt}"><i class="bi bi-pencil"></i> <c:out value="${auditUpdatedAt}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Approved At --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Approved At</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditApprovedAt}"><i class="bi bi-clock-history"></i> <c:out value="${auditApprovedAt}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Tenant --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Tenant</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditTenantName}"><i class="bi bi-building"></i> <c:out value="${auditTenantName}"/></c:when>
                            <c:when test="${not empty sessionScope.tenantName}"><i class="bi bi-building"></i> <c:out value="${sessionScope.tenantName}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>

            <%-- Branch --%>
            <div class="col-md-3">
                <div class="cbs-audit-field">
                    <small class="text-muted d-block">Branch</small>
                    <strong>
                        <c:choose>
                            <c:when test="${not empty auditBranchCode}"><i class="bi bi-geo-alt"></i> <c:out value="${auditBranchCode}"/></c:when>
                            <c:when test="${not empty sessionScope.branchCode}"><i class="bi bi-geo-alt"></i> <c:out value="${sessionScope.branchCode}"/></c:when>
                            <c:otherwise><span class="text-muted">--</span></c:otherwise>
                        </c:choose>
                    </strong>
                </div>
            </div>
        </div>
    </div>
</div>
