<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-${not empty editUser ? 'gear' : 'plus'}"></i>
        <c:choose>
            <c:when test="${not empty editUser}">Edit User: <c:out value="${editUser.username}"/></c:when>
            <c:otherwise>Create New User</c:otherwise>
        </c:choose>
    </h3>
    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Users</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}"><div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>

<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-person-lines-fill"></i> User Details</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/admin/users/${not empty editUser ? editUser.id.toString().concat('/edit') : 'create'}"
              onsubmit="return confirm('${not empty editUser ? 'Update' : 'Create'} this user?');">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <div class="row g-3 mb-3">
                <c:if test="${empty editUser}">
                <div class="col-md-4">
                    <label class="form-label fw-bold">Username <span class="text-danger">*</span></label>
                    <input type="text" name="username" class="form-control" required placeholder="e.g. loan_maker_01" maxlength="50"/>
                    <small class="text-muted">Unique login identifier. Cannot be changed after creation.</small>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-bold">Password <span class="text-danger">*</span></label>
                    <input type="password" name="password" class="form-control" required minlength="8" placeholder="Min 8 characters"/>
                </div>
                </c:if>
                <div class="col-md-4">
                    <label class="form-label fw-bold">Full Name <span class="text-danger">*</span></label>
                    <input type="text" name="fullName" class="form-control" required maxlength="100"
                           value="${not empty editUser ? fn:escapeXml(editUser.fullName) : ''}" placeholder="e.g. Rajesh Sharma"/>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-md-4">
                    <label class="form-label">Email</label>
                    <input type="email" name="email" class="form-control" maxlength="100"
                           value="${not empty editUser ? fn:escapeXml(editUser.email) : ''}" placeholder="user@bank.com"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Phone</label>
                    <input type="text" name="phone" class="form-control" maxlength="20"
                           value="${not empty editUser ? fn:escapeXml(editUser.phone) : ''}" placeholder="+91-XXXXXXXXXX"/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Branch Code</label>
                    <input type="text" name="branchCode" class="form-control" maxlength="10"
                           value="${not empty editUser ? fn:escapeXml(editUser.branchCode) : ''}" placeholder="e.g. HQ001"/>
                </div>
            </div>

            <c:if test="${not empty editUser}">
            <div class="row g-3 mb-3">
                <div class="col-md-4">
                    <label class="form-label">New Password <small class="text-muted">(leave blank to keep current)</small></label>
                    <input type="password" name="newPassword" class="form-control" minlength="8" placeholder="New password (optional)"/>
                </div>
            </div>
            </c:if>

            <hr/>
            <h6 class="fw-bold mb-3"><i class="bi bi-shield-lock"></i> Role Assignment</h6>
            <div class="row g-2 mb-3">
                <c:forEach var="role" items="${allRoles}">
                <div class="col-md-3">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" name="roleIds" value="${role.id}" id="role_${role.id}"
                            <c:if test="${not empty editUser}">
                                <c:forEach var="userRole" items="${editUser.roles}">
                                    <c:if test="${userRole.id == role.id}">checked</c:if>
                                </c:forEach>
                            </c:if>
                        />
                        <label class="form-check-label" for="role_${role.id}">
                            <span class="badge bg-primary"><c:out value="${role.name}"/></span>
                            <c:if test="${not empty role.description}">
                            <br/><small class="text-muted"><c:out value="${role.description}"/></small>
                            </c:if>
                        </label>
                    </div>
                </div>
                </c:forEach>
            </div>

            <hr/>
            <div class="d-flex justify-content-between">
                <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary">Cancel</a>
                <button type="submit" class="btn btn-primary">
                    <i class="bi bi-check-circle"></i>
                    <c:choose>
                        <c:when test="${not empty editUser}">Update User</c:when>
                        <c:otherwise>Create User</c:otherwise>
                    </c:choose>
                </button>
            </div>
        </form>
    </div>
</div>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    User management operations are audit-logged. Role changes require ADMIN/TENANT_ADMIN privileges per CBS governance.
</div>

<%@ include file="../layout/footer.jsp" %>
