<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-building"></i> Tenant Configuration
        <small class="text-muted">| ${tenantCount} tenant(s)</small>
    </h3>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- Create Tenant Form (Maker action — SUPER_ADMIN / ADMIN only) --%>
<c:if test="${sessionScope.isAdmin || sessionScope.isSuperAdmin}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-plus-circle"></i> Onboard New Tenant</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/admin/tenants/create" class="row g-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="col-md-3">
                <label for="tenantCode" class="form-label cbs-field-required">Tenant Code</label>
                <input type="text" class="form-control" id="tenantCode" name="tenantCode" required maxlength="20"
                       pattern="^[A-Z0-9_-]+$" placeholder="e.g. TENANT-003" title="Uppercase alphanumeric with hyphens/underscores">
            </div>
            <div class="col-md-3">
                <label for="tenantName" class="form-label cbs-field-required">Tenant Name</label>
                <input type="text" class="form-control" id="tenantName" name="tenantName" required maxlength="100" placeholder="e.g. Partner Bank">
            </div>
            <div class="col-md-3">
                <label for="regulatoryCode" class="form-label">RBI Regulatory Code</label>
                <input type="text" class="form-control" id="regulatoryCode" name="regulatoryCode" maxlength="50"
                       placeholder="e.g. RBI/2024/BANK/001" pattern="^RBI/[0-9]{4}/[A-Z]+/[0-9]{3,}$">
            </div>
            <div class="col-md-3">
                <label for="baseCurrency" class="form-label">Base Currency</label>
                <select class="form-select" id="baseCurrency" name="baseCurrency">
                    <option value="INR" selected>INR</option>
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                </select>
            </div>
            <div class="col-md-2">
                <label for="country" class="form-label">Country</label>
                <input type="text" class="form-control" id="country" name="country" value="IN" maxlength="5" placeholder="e.g. IN">
            </div>
            <div class="col-md-3">
                <label for="timezone" class="form-label">Timezone</label>
                <input type="text" class="form-control" id="timezone" name="timezone" value="Asia/Kolkata" maxlength="50">
            </div>
            <div class="col-md-2">
                <label for="effectiveFrom" class="form-label">Effective From</label>
                <input type="date" class="form-control" id="effectiveFrom" name="effectiveFrom">
            </div>
            <div class="col-md-2">
                <label for="multiBranchEnabled" class="form-label">Multi-Branch</label>
                <select class="form-select" id="multiBranchEnabled" name="multiBranchEnabled">
                    <option value="false" selected>No</option>
                    <option value="true">Yes</option>
                </select>
            </div>
            <div class="col-md-3">
                <label for="remarks" class="form-label">Remarks</label>
                <input type="text" class="form-control" id="remarks" name="remarks" maxlength="500" placeholder="Onboarding notes">
            </div>
            <div class="col-md-3 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100" onclick="return confirm('Create new tenant? This action will be audited.')">
                    <i class="bi bi-plus-circle"></i> Create Tenant
                </button>
            </div>
        </form>
    </div>
</div>
</c:if>

<%-- Tenant Registry Table --%>
<div class="card shadow">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-building"></i> Tenant Registry</h5></div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>Tenant Code</th>
                        <th>Tenant Name</th>
                        <th>Status</th>
                        <th>Business Date</th>
                        <th>Day Status</th>
                        <th>EOD Status</th>
                        <th>Currency</th>
                        <th>Country</th>
                        <th>Regulatory Code</th>
                        <th>Effective From</th>
                        <th>Created</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="tenant" items="${tenants}">
                        <tr>
                            <td>${tenant.id}</td>
                            <td><code><c:out value="${tenant.tenantCode}"/></code></td>
                            <td><c:out value="${tenant.tenantName}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${tenant.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                    <c:when test="${tenant.status == 'INITIALIZING'}"><span class="badge bg-info">INITIALIZING</span></c:when>
                                    <c:when test="${tenant.status == 'INACTIVE'}"><span class="badge bg-danger">INACTIVE</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary"><c:out value="${tenant.status}"/></span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${tenant.currentBusinessDate}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${tenant.dayStatus == 'OPEN'}"><span class="badge bg-success">OPEN</span></c:when>
                                    <c:when test="${tenant.dayStatus == 'DAY_CLOSING'}"><span class="badge bg-warning text-dark">DAY_CLOSING</span></c:when>
                                    <c:when test="${tenant.dayStatus == 'CLOSED'}"><span class="badge bg-secondary">CLOSED</span></c:when>
                                    <c:otherwise><span class="badge bg-danger"><c:out value="${tenant.dayStatus}"/></span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${tenant.eodStatus == 'COMPLETED'}"><span class="badge bg-success">COMPLETED</span></c:when>
                                    <c:when test="${tenant.eodStatus == 'IN_PROGRESS'}"><span class="badge bg-warning text-dark">IN_PROGRESS</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary"><c:out value="${tenant.eodStatus}" default="NOT_STARTED"/></span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${tenant.baseCurrency}" default="INR"/></td>
                            <td><c:out value="${tenant.country}" default="IN"/></td>
                            <td><code><c:out value="${tenant.regulatoryCode}" default="--"/></code></td>
                            <td><c:out value="${tenant.effectiveFrom}" default="--"/></td>
                            <td><small><c:out value="${tenant.createdAt}"/></small></td>
                            <td>
                                <c:if test="${sessionScope.isAdmin || sessionScope.isSuperAdmin}">
                                    <c:if test="${tenant.status != 'ACTIVE'}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/tenants/${tenant.id}/activate" style="display:inline;">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                            <button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Activate this tenant?')" title="Activate"><i class="bi bi-check-circle"></i></button>
                                        </form>
                                    </c:if>
                                    <c:if test="${tenant.status == 'ACTIVE'}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/tenants/${tenant.id}/deactivate" style="display:inline;">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                            <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Deactivate this tenant? Business day must be CLOSED.')" title="Deactivate"><i class="bi bi-x-circle"></i></button>
                                        </form>
                                    </c:if>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty tenants}">
                        <tr><td colspan="13" class="text-center text-muted py-4"><i class="bi bi-building" style="font-size: 2rem;"></i><br>No tenants configured.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Governance Note --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All tenant lifecycle operations are logged in the audit trail. Deactivation requires business day to be CLOSED.
</div>

<%@ include file="../layout/footer.jsp" %>
