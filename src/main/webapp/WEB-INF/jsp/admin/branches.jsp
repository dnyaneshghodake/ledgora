<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-geo-alt"></i> Branch (SOL) Management
        <small class="text-muted">| ${branchCount} branch(es)</small>
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

<%-- Create Branch Form --%>
<c:if test="${sessionScope.isAdmin || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create New Branch (SOL)</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/admin/branches/create" class="row g-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="col-md-2">
                <label for="branchCode" class="form-label cbs-field-required">SOL ID</label>
                <input type="text" class="form-control" id="branchCode" name="branchCode" required maxlength="10"
                       pattern="^[A-Z0-9]+$" placeholder="e.g. BR003" title="Uppercase alphanumeric, max 10 chars">
            </div>
            <div class="col-md-3">
                <label for="name" class="form-label cbs-field-required">Branch Name</label>
                <input type="text" class="form-control" id="name" name="name" required maxlength="100" placeholder="e.g. Midtown Branch">
            </div>
            <div class="col-md-2">
                <label for="ifscCode" class="form-label">IFSC Code</label>
                <input type="text" class="form-control" id="ifscCode" name="ifscCode" maxlength="11"
                       pattern="^[A-Z]{4}0[A-Z0-9]{6}$" placeholder="e.g. LDGR0000003" title="11-char IFSC format">
            </div>
            <div class="col-md-2">
                <label for="branchType" class="form-label">Branch Type</label>
                <select class="form-select" id="branchType" name="branchType">
                    <option value="BRANCH">BRANCH</option>
                    <option value="HEAD_OFFICE">HEAD_OFFICE</option>
                    <option value="REGIONAL_OFFICE">REGIONAL_OFFICE</option>
                    <option value="EXTENSION_COUNTER">EXTENSION_COUNTER</option>
                    <option value="ATM_SITE">ATM_SITE</option>
                </select>
            </div>
            <div class="col-md-3">
                <label for="address" class="form-label">Address</label>
                <input type="text" class="form-control" id="address" name="address" maxlength="500" placeholder="e.g. 3rd Avenue, Midtown">
            </div>
            <div class="col-md-2">
                <label for="city" class="form-label">City</label>
                <input type="text" class="form-control" id="city" name="city" maxlength="50" placeholder="e.g. Mumbai">
            </div>
            <div class="col-md-2">
                <label for="state" class="form-label">State</label>
                <input type="text" class="form-control" id="state" name="state" maxlength="50" placeholder="e.g. Maharashtra">
            </div>
            <div class="col-md-2">
                <label for="pincode" class="form-label">Pincode</label>
                <input type="text" class="form-control" id="pincode" name="pincode" maxlength="10"
                       pattern="^[0-9]{6}$" placeholder="e.g. 400001" title="6-digit pincode">
            </div>
            <div class="col-md-2">
                <label for="micrCode" class="form-label">MICR Code</label>
                <input type="text" class="form-control" id="micrCode" name="micrCode" maxlength="9"
                       pattern="^[0-9]{9}$" placeholder="e.g. 400002001" title="9-digit MICR code">
            </div>
            <div class="col-md-2">
                <label for="contactPhone" class="form-label">Phone</label>
                <input type="text" class="form-control" id="contactPhone" name="contactPhone" maxlength="20" placeholder="e.g. 022-12345678">
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <button type="submit" class="btn btn-primary w-100" onclick="return confirm('Create new branch? This action will be audited.')">
                    <i class="bi bi-plus-circle"></i> Create Branch
                </button>
            </div>
        </form>
    </div>
</div>
</c:if>

<%-- Branch Registry Table --%>
<div class="card shadow">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-geo-alt"></i> Branch Registry (SOL Master)</h5></div>
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                    <tr>
                        <th>SOL ID</th>
                        <th>Branch Name</th>
                        <th>Type</th>
                        <th>IFSC</th>
                        <th>City</th>
                        <th>State</th>
                        <th>Pincode</th>
                        <th>Status</th>
                        <th>Created</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="branch" items="${branches}">
                        <tr>
                            <td><code><c:out value="${branch.branchCode}"/></code></td>
                            <td><c:out value="${branch.branchName}" default="${branch.name}"/></td>
                            <td><span class="badge bg-info"><c:out value="${branch.branchType}" default="BRANCH"/></span></td>
                            <td><code><c:out value="${branch.ifscCode}" default="--"/></code></td>
                            <td><c:out value="${branch.city}" default="--"/></td>
                            <td><c:out value="${branch.state}" default="--"/></td>
                            <td><c:out value="${branch.pincode}" default="--"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${branch.isActive}"><span class="badge bg-success">ACTIVE</span></c:when>
                                    <c:otherwise><span class="badge bg-danger">INACTIVE</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td><small><c:out value="${branch.createdAt}"/></small></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/admin/branches/${branch.id}" class="btn btn-sm btn-outline-primary" title="View Details"><i class="bi bi-eye"></i></a>
                                <c:if test="${sessionScope.isAdmin || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
                                    <c:if test="${branch.isActive}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/branches/${branch.id}/deactivate" style="display:inline;">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                            <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Deactivate this branch?')" title="Deactivate"><i class="bi bi-x-circle"></i></button>
                                        </form>
                                    </c:if>
                                    <c:if test="${!branch.isActive}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/branches/${branch.id}/activate" style="display:inline;">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                            <button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Activate this branch?')" title="Activate"><i class="bi bi-check-circle"></i></button>
                                        </form>
                                    </c:if>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty branches}">
                        <tr><td colspan="10" class="text-center text-muted py-4"><i class="bi bi-geo-alt" style="font-size: 2rem;"></i><br>No branches configured.</td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- Governance Note --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All branch lifecycle operations are logged in the audit trail. IFSC and MICR codes are RBI-mandated identifiers.
</div>

<%@ include file="../layout/footer.jsp" %>
